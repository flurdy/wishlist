package models

import com.google.inject.ImplementedBy
import enumeratum._
import enumeratum.EnumEntry._
import javax.inject._


@ImplementedBy(classOf[DefaultFeatureToggles])
trait FeatureToggles extends WithApplicationConfig {

   def isEnabledByName(featureName: String) =
         appConfig.findBoolean(s"feature.$featureName.enabled").getOrElse(false)

   def isEnabled(featureToggle: FeatureToggle) = isEnabledByName(featureToggle.entryName.toLowerCase)

}

@Singleton
class DefaultFeatureToggles @Inject() (val appConfig: ApplicationConfig) extends FeatureToggles

sealed trait FeatureToggle extends EnumEntry {
   def isEnabled()(implicit featureToggles: FeatureToggles)  = featureToggles.isEnabled(this)
   def isDisabled()(implicit featureToggles: FeatureToggles) = !isEnabled()
}

object FeatureToggle extends Enum[FeatureToggle] {
   val values = findValues
   case object EmailVerification extends FeatureToggle with Dotcase
   case object Gravatar extends FeatureToggle with Dotcase
}

trait WithFeatureToggles {
   implicit def featureToggles: FeatureToggles
}
