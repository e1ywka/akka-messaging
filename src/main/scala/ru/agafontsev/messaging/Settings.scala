package ru.agafontsev.messaging

import akka.actor._
import com.typesafe.config.{Config, ConfigException}

import scala.util.{Failure, Success, Try}

class SettingsImpl(config: Config) extends Extension {
  val PingMulticastInterface = config.getString("ru.agafontsev.ping.interface")
  val PingMulticastGroup = config.getString("ru.agafontsev.ping.group")
  val PingMulticastPort = config.getInt("ru.agafontsev.ping.port")
  val ServiceHost = Try(config.getString("ru.agafontsev.service.host")) match {
    case Success(host) => host
    case Failure(e: ConfigException.Missing) => "127.0.0.1"
    case Failure(e) => throw e
  }
  val ServicePort = config.getInt("ru.agafontsev.service.port")
}

object Settings extends ExtensionId[SettingsImpl] with ExtensionIdProvider {
  override def createExtension(system: ExtendedActorSystem): SettingsImpl =
    new SettingsImpl(system.settings.config)

  override def lookup() = Settings
}
