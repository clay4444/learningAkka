package org.clay.AkkaTest

import com.typesafe.config.ConfigFactory
import slick.jdbc.GetResult
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}

// define table structure(we can define many tables bindings)
object SlickDB{

  // table name: scala_model
  case class UserInfo(id: Long, name: String, age: Int)

  implicit val getAssembledObjectResult = GetResult(r => UserInfo(r.nextLong(), r.nextString(),r.nextInt()))

  class SlickModelTable(tag: Tag) extends Table[UserInfo](tag,"scala_model"){

    // define column attribute
    def id = column[Long]("id",O.PrimaryKey, O.AutoInc)  // make sure here is primary key and auto inc(return column needed)
    def name = column[String]("name")
    def age = column[Int]("age")
    def * = (id,name,age) <> (UserInfo.tupled, UserInfo.unapply)  //tupled接收一个tuple，返回一个UserInfo，unapply接收一个UserInfo，返回一个Tuple
  }

  def slick_table = TableQuery(tag => new SlickModelTable(tag))
}

import SlickDB._

object SlickTest extends App {

  // config database
  //1.配置url
  val db = Database.forURL(
    url = "jdbc:mysql://localhost:3306/test?useUnicode=true&characterEncoding=UTF-8&useSSL=false",
    driver = "com.mysql.jdbc.Driver",
    user = "root",
    password = "tiger"
  )

  //2.配置config
  val config = ConfigFactory.load()
  val db2 = Database.forConfig("mysql_db",config.getConfig("db"))

  // query all
  // slick run returns a future, we can use andThen to get async response and use Await.result to get result
  // usage1
  val query_action = slick_table.result
  val res1 = db.run(query_action).andThen {
    case Success(_) => println("query success")
    case Failure(e) => println("query failed ", e.getMessage)
  }

  // usage2
  db.run(slick_table.result).map {
    result => println(result)
  }

  // block thread to get select result
  Await.result(res1, 10 seconds) // specify the timeout


  // query by condition
  val res2 = Await.result(db.run(slick_table.filter(_.age > 20).result), Duration.Inf)
  println(s"res2: ${res2}")

  // add（only 1 record)
  /*val user1 = UserInfo(6L, "scarllet", 19)
  val res3 = Await.result(db.run(slick_table += user1), Duration.Inf) // return the insert numbers: 1, so no need to return

  println(s"res3: ${res3}")  //影响的行数，1*/


  // add(batch records)
  /*val user1 = UserInfo(6L, "scarllet", 19)
  val user2 = UserInfo(7L, "mary", 21)
  val newArray = Seq[UserInfo](user1, user2)
  val res3 = Await.result(db.run(slick_table ++= newArray), Duration.Inf)*/ // return the insert numbers: 2, so no need to return

  // update
  val new_user = UserInfo(3L, "tashaxing", 23)
  val res4 = Await.result(db.run(slick_table.filter(_.id === new_user.id).update(new_user)), Duration.Inf) // return effected row numbers

  // delete
  val res5 = Await.result(db.run(slick_table.filter(_.name === "tony").delete), Duration.Inf)


  // 设置插入之后要返回的列
  val user = UserInfo(0, "ethan", 21)
  val save_sql = (slick_table returning slick_table.map(_.id)) += UserInfo(0, "ethan", 21)
  val user_id = Await.result(db.run(save_sql), Duration.Inf) // return created id(新插入的值的id)
  println(s"user_id: ${user_id}")


  //into
  val userWithId =
    (slick_table returning slick_table.map(_.id)
      //into:Specifies a mapping from inserted values and generated keys to a desired value.
      into ((user,newId) => user.copy(id=newId)  //为插入的值和return返回的值 到 我们想要的值之间做一个映射（我们这里想返回一个完整的UserInfo）
      ) += UserInfo(2, "update",0))  //这里 id 没用，

  //和上一个rerurn的区别是上面只返回了user_id，这里返回了一个完整的UserInfo
  val userWithIdRes = Await.result(db.run(userWithId), Duration.Inf)
  println(s"userWithIdRes: ${userWithIdRes}")

  // ---- use sql

  // query sql
  val res6 = Await.result(db.run(sql"""select id,name,age from scala_model""".as[UserInfo]),Duration.Inf)   //def as[R](implicit rconv: GetResult[R])
  println(s"res6: ${res6}")

  // insert sql
  /*val id = 10L
  val name = "wilson"
  val age = 29
  val res7 = Await.result(db.run(sqlu"""insert into scala_model values($id, $name, $age)"""),Duration.Inf)
  println(s"res7: ${res7}")*/  //返回插入的数量

  // update sql
  val res8 = Await.result(db.run(sqlu"""update scala_model set name='lily' where id=4"""), Duration.Inf)
  println(s"res8: ${res8}")  //更新的数量


  // delete sql
  val res9 = Await.result(db.run(sqlu"""delete from scala_model where name='ethan'"""), Duration.Inf)
  println(s"res9: ${res9}")   //删除的数量
}