package models

case class AdsenseDetails(client: String, slot: String)

trait WithAdsense {

   def appConfig: ApplicationConfig

   implicit def adsenseDetails: Option[AdsenseDetails] =
      for {
         client <- appConfig.findString("adsense.client")
         slot <- appConfig.findString("adsense.slot")
      } yield AdsenseDetails(client,slot)

}
