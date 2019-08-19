package org.clay.test1

import java.util.concurrent.atomic.AtomicLong

import akka.actor.{Actor, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider}

/**
  * 定义一个对某件事发生的次数进行统计的扩展
  */
class CountExtensionImpl extends Extension {
  //Since this Extension is a shared instance  在一个ActorSystem中共享，所以需要保证线程安全
  // per ActorSystem we need to be threadsafe
  private val counter = new AtomicLong(0)

  //This is the operation this Extension provides   //扩展提供的功能
  def increment() = counter.incrementAndGet()
}

/**
  * 为这个扩展指定一个ExtensionId，用来获取它的实例
  */
object CountExtension
  extends ExtensionId[CountExtensionImpl]
    with ExtensionIdProvider {
  //The lookup method is required by ExtensionIdProvider, lookup 方法是ExtensionIdProvider需要的，
  // so we return ourselves here, this allows us        在这个方法里我们返回自己，这个方法的作用是配置我们的扩展在当前的ActorSystem
  // to configure our extension to be loaded when      启动的时候被加载，
  // the ActorSystem starts up
  override def lookup = CountExtension

  //This method will be called by Akka
  // to instantiate our Extension   //这个方法将会被Akka调用，用来实例化我们定义的扩展，
  override def createExtension(system: ExtendedActorSystem) = new CountExtensionImpl

  /**
    * Java API: retrieve the Count extension for the given system.
    * Java API: 为指定的ActorSystem返回我们定义的扩展
    */
  override def get(system: ActorSystem): CountExtensionImpl = super.get(system)
}

/**
  * 使用这个扩展
  */
class MyActor extends Actor {
  def receive = {
    case someMessage:_ =>
      CountExtension(context.system).increment()
  }
}

/**
  * 可以将扩展藏在 trait 里
  */
trait Counting { self: Actor =>
  def increment() = CountExtension(context.system).increment()
}
class MyCounterActor extends Actor with Counting {
  def receive = {
    case someMessage => increment()
  }
}


class TestExtension {

}
