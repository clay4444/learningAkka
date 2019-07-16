package org.clay.AkkaTest

import com.typesafe.config.ConfigFactory
import slick.driver.MySQLDriver.api._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.util.{Failure, Success}

// define table structure(we can define many tables bindings)
object SlickDB{

  // table name: scala_model
  case class UserInfo(id: Long, name: String, age: Int)

  class SlickModelTable(tag: Tag) extends Table[UserInfo](tag,"scala_model"){

    // define column attribute
    def id = column[Long]("id",O.PrimaryKey, O.AutoInc)  // make sure here is primary key and auto inc(return column needed)
    def name = column[String]("name")
    def age = column[Int]("age")
    def * = (id,name,age) <> (UserInfo.tupled, UserInfo.unapply)
  }

  def slick_table = TableQuery[SlickModelTable]
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
  val res2 = Await.result(db.run(slick_table.filter(_.age > 25).result), Duration.Inf)
}
