import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, Uri}
import akka.http.scaladsl.unmarshalling.Unmarshal
import akka.util.ByteString
import com.typesafe.config.ConfigFactory
import cronish.dsl._
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{ChatActions, Polling, TelegramBot}
import info.mukel.telegrambot4s.methods.SendPhoto
import info.mukel.telegrambot4s.models.{InputFile, Message}


/**
  * Created by phompang on 7/13/2017 AD.
  */
object TMDWeatherBot extends TelegramBot with Polling with Commands with ChatActions {
  var regisUser: Map[Long, Message] = Map()

  override def token: String = ConfigFactory.load().getString("bot.token")

  onCommand("/register") { implicit message =>
    regisUser += (message.source -> message)
    reply("register complete!")
  }

  onCommand("/unregister") { implicit message =>
    regisUser -= (message.source)
    reply("unregister complete!")
  }

  onCommand("/weather") { implicit message =>
    withArgs { implicit args =>
      var radar = args.mkString(" ")
      radar = if (radar != "") radar else "nongkhame"
      getRadar(message, radar)
    }
  }

  def getRadar(message: Message, radar: String) = {
    for {
      response <- Http().singleRequest(HttpRequest(uri = Uri(RadarUrl.urls(radar))))
      if response.status.isSuccess()
      bytes <- Unmarshal(response).to[ByteString]
    } /* do */ {
      val photo = InputFile("weather.jpg", bytes)
      uploadingPhoto(message) // Hint the user
      request(SendPhoto(Left(message.source), Left(photo)))
    }
  }

  def publishRadar() = {
    for ((_, message) <- regisUser) {
      getRadar(message, "nongkhame")
    }
  }

  onCommand("/list") { implicit message =>
    reply(RadarUrl.urls.keySet.mkString("\n"))
  }

  onCommand("/ping") { implicit message =>
    reply("Pong!")
  }
}

object Main extends App {
//  val storage = StorageOptions.getDefaultInstance().getService();
//
//  val bucket = storage.create(BucketInfo.of("aaaa-a0ba5.appspot.com"));
//
//  println(bucket.getName)

  val bot = TMDWeatherBot
  bot.run()

  val publish = task {
    bot.publishRadar()
  }
  publish executes "Every 30 minutes at 4pm to 9pm"
  publish executes "Every 30 minutes at 6am to 9am"

}
