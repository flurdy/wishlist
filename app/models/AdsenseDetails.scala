package models

case class AdsenseDetails(client: String, slot: String)

trait WithAdsense {

   def appConfig: ApplicationConfig

   def adsenseSlot: Option[String]

   implicit def adsenseDetails: Option[AdsenseDetails] =
      for {
         slotName <- adsenseSlot
         client   <- appConfig.findString("adsense.client")
         slot     <- appConfig.findString(s"adsense.slot.${slotName}")
      } yield AdsenseDetails(client,slot)

}
