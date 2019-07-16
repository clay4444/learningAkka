package org.clay.AkkaTest

import com.typesafe.config.ConfigFactory
import slick.jdbc.MySQLProfile.api._

object SlickDB{

  // table name: scala_model
  case class UserInfo(id: Long, name: String, age: Int)

  class SlickModelTable(tag: Tag) extends Table[UserInfo](tag,"scala_model"){

    
  }
}

object SlickTest extends App {

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
}
