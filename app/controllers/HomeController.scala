package controllers

import javax.inject._

import akka.actor.ActorSystem
import play.api.libs.json.Json
import play.api.mvc._
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.macros.derive._
import sangria.marshalling.playJson._
import sangria.parser.{QueryParser, SyntaxError}
import sangria.schema._

import scala.concurrent.Future
import scala.util.{Failure, Success}

@Singleton
class HomeController @Inject()(cc: ControllerComponents, system: ActorSystem) extends AbstractController(cc) {
  // TODO: Investigate the relationship between this and the implicit ExecutionContext needed by the Executor.
  import system.dispatcher

  case class Picture(width: Int, height: Int, url: Option[String])

  // TODO: Investigate why the documentation puts this as implicit.
  implicit val PictureType = deriveObjectType[Unit, Picture](
    ObjectTypeDescription("The product picture."),
    DocumentField("url", "Picture CDN URL.")
  )

  trait Identifiable {
    def id: String
  }

  val IdentifiableType = InterfaceType(
    "Identifiable",
    "Entity that can be identified.",

    fields[Unit, Identifiable](
      Field("id", StringType, resolve = _.value.id)
    )
  )

  case class Product(id: String, name: String, description: String) extends Identifiable {
    def picture(size: Int): Picture =
      Picture(width = size, height = size, url = Some(s"//cdn.com/$size/$id.jpg"))
  }

  val ProductType = deriveObjectType[Unit, Product](
    Interfaces(IdentifiableType),
    IncludeMethods("picture")
  )

  class ProductRepository {
    private val Products = List(
      Product("1", "Cheesecake", "Tasty"),
      Product("2", "Health Potion", "+50 HP")
    )

    def product(id: String): Option[Product] = Products find { _.id == id }

    def products: List[Product] = Products
  }

  val Id = Argument("id", StringType)

  val QueryType = ObjectType(
    "Query",

    fields[ProductRepository, Unit](
      Field("product", OptionType(ProductType),
        description = Some("Returns a product with specific `id`."),
        arguments = Id :: Nil,
        resolve = c => c.ctx.product(c arg Id)),
      Field("products", ListType(ProductType),
        description = Some("Returns a list of all available products."),
        resolve = _.ctx.products)
    )
  )

  val schema = Schema(QueryType)

  val query = """
    query MyProduct {
      product(id: "2") {
        name
        description

        picture(size: 500) {
          width, height, url
        }
      }

      products {
        name
      }
    }
  """

  def index() = Action.async { request =>
    QueryParser.parse(query) match {
      case Success(queryAst) =>
        Executor.execute(schema, queryAst, new ProductRepository)
          .map(Ok(_))
          .recover {
            case error: QueryAnalysisError ⇒ BadRequest (error.resolveError)
            case error: ErrorWithResolver ⇒ InternalServerError (error.resolveError)
          }
      case Failure(error: SyntaxError) =>
        Future.successful(BadRequest(Json.obj("error" -> error.getMessage)))

    }
  }
}
