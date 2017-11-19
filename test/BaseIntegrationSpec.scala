package com.flurdy.wishlist

import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.{Helpers, TestServer}
import play.api.libs.ws.WSClient
import org.scalatest.{BeforeAndAfterAll, Suite}


trait WithTestServer {

   var app: Application = _
   private var server: TestServer = _
   val port = Helpers.testServerPort
   lazy val baseUrl: String = s"http://localhost:$port"

   var applicationConfiguration = Map("play.http.router" -> "testonly.Routes",
                                      "com.flurdy.wishlist.feature.email.verification.enabled" -> false)

   def startServer() = {
      app = new  GuiceApplicationBuilder()
         .configure(applicationConfiguration)
         .build()
      server = TestServer(port, app)
      server.start()
   }

   def stopServer() = server.stop()

   def getWsClient() = app.injector.instanceOf[WSClient]

}

trait StartAndStopServer extends Suite with BeforeAndAfterAll with WithTestServer {

   override def beforeAll = startServer()

   override def afterAll = stopServer()

}

trait IntegrationHelper {
   def baseUrl: String
   def getWsClient(): WSClient

   def wsWithSession(url: String, session: Option[String]) =
      session.fold( getWsClient().url(url) ){ s =>
         getWsClient().url(url).withHttpHeaders("cookie" -> s"PLAY_SESSION=$s")
      }
}
