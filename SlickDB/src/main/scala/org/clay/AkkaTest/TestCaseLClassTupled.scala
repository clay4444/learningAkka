package org.clay.AkkaTest

case class User(name:String,email:String,age:Int)

object TestCaseLClassTupled extends App {

  val userLikeData = ("John","John@doe.com",33)
  val userLikeData1 = ("John","John@doe.com",33)

  //tupled,接收一个tuple作为参数，返回一个case class 实例
  val user = User.tupled(userLikeData)
  val user1 = User.tupled(userLikeData1)

  //copy
  val user3 = user.copy(age = 11)
  println(user3)

  //unapply
  //自动生成 unapply  用于模式匹配
  def matchPrint(x: AnyRef) = {
    x match {
      case User(name,email,age) => println(s"match name: ${name}")
      case _ => println("It's not User")
    }
  }
  matchPrint(user3)

  //解构
  val User(name,_,_) = user
  println(s"name-> ${name}")

  //解构出 tuple
  val transform: User =>Option[(String,String,Int)] = {
    User.unapply _
  }
  val userTuple = transform(user)

  println(s"userTuple: ${userTuple}")

  println(user == user1)  //true
}
