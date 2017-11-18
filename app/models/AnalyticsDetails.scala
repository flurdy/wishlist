package models

case class AnalyticsDetails(accountId: String)

trait WithAnalytics {

   def appConfig: ApplicationConfig

   implicit def analyticsDetails: Option[AnalyticsDetails] = appConfig.findString("analytics.id").map(AnalyticsDetails)

}
