package io.scalac.slack.example

import akka.actor.{ActorRef, ActorContext, Props, ActorSystem}
import io.scalac.slack.api.{BotInfo, Start}
import io.scalac.slack.bots.system.{HelpBot, CommandsRecognizerBot}
import io.scalac.slack.common.actors.SlackBotActor
import io.scalac.slack.common.{UsersStorage, Shutdownable}
import io.scalac.slack.{Config => SlackConfig, BotModules, MessageEventBus}
import io.scalac.slack.websockets.{WebSocket, WSActor}

object BotRunner extends Shutdownable {
  val system = ActorSystem("SlackBotSystem")
  val eventBus = new MessageEventBus
  val slackBot = system.actorOf(Props(classOf[SlackBotActor], new ExampleBotsBundle(), eventBus, this, None), "slack-bot")

  var botInfo: Option[BotInfo] = None

  def main(args: Array[String]) {
    println("SlackBot started")
    println("With api key: " + SlackConfig.apiKey)

    try {
      slackBot ! Start

      system.awaitTermination()
      println("Shutdown successful...")
    } catch {
      case e: Exception =>
        println("An unhandled exception occurred...", e)
        system.shutdown()
        system.awaitTermination()
    }
  }

  sys.addShutdownHook(shutdown())

  override def shutdown(): Unit = {
    slackBot ! WebSocket.Release
    system.shutdown()
    system.awaitTermination()
  }

  class ExampleBotsBundle() extends BotModules {
    override def registerModules(context: ActorContext, websocketClient: ActorRef) = {
      context.actorOf(Props(classOf[CommandsRecognizerBot], eventBus), "commandProcessor")
      context.actorOf(Props(classOf[HelpBot], eventBus), "helpBot")
      context.actorOf(Props(classOf[CalculatorBot], eventBus), "CalculatorBot")
    }
  }
}
