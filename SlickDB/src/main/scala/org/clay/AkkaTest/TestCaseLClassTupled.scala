package org.clay.AkkaTest

case class User(name:String,email:String,age:Int)

object TestCaseLClassTupled extends App {

  val userLikeData = ("John","John@doe.com",33)
  val user = User.tupled(userLikeData)
  println(user)
}
