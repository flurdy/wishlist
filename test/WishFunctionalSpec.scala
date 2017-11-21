package com.flurdy.wishlist

import org.scalatest._
import org.scalatest.time._
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test._
import play.api.test.Helpers.{ GET => GET_REQUEST, _ }


class WishFunctionalSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite 
with ChromeFactory { 

   var applicationConfiguration = Map("play.http.router" -> "testonly.Routes",
                                      "com.flurdy.wishlist.feature.email.verification.enabled" -> true)

   override def fakeApplication() =
      new GuiceApplicationBuilder()
         .configure(applicationConfiguration)   
         .build()

   "Can create a wish" must {

     "have a front page" in {
        go to s"http://localhost:$port/logout"
        go to s"http://localhost:$port/"
        pageTitle mustBe "Wish"
        find(cssSelector("#register-box button")).value.text mustBe "register"
     }
    
     "pre fill register form with email" in {
       click on cssSelector("#register-box input")
       enter("johnsmith@example.com")
       click on cssSelector("#register-box button")
       eventually { 
          pageTitle mustBe "Wish register" 
          textField("username").value mustBe "johnsmith@example.com"
          textField("email").value mustBe "johnsmith@example.com"
       }
    }
    
    "be able to register" in {
       click on textField("fullname")
       textField("fullname").value = "John Smith"
       textField("username").value = "johnsmith"
       textField("email").value = "johnsmith@example.com"
       pwdField("password").value = "simsalabim"
       pwdField("confirm").value = "simsalabim"
       submit()
       eventually { 
          pageTitle mustBe "Wish" 
          find(cssSelector(".alert-success")).value.text must include ("Welcome, you have successfully registered.") 
       }
    }

    "verify" in {
      go to s"http://localhost:$port/test-only/recipient/johnsmith/verify/find"
       eventually { 
          find(cssSelector(".alert-success")).value.text must include ("Email address verified. Please log in")           
       }
    }

    "log in" in {
       click on textField("username")
       textField("username").value = "johnsmith"
       pwdField("password").value = "simsalabim"
       submit()
       eventually { 
          pageTitle mustBe "Wish" 
          find(cssSelector(".alert-info")).value.text must include ("You have logged in") 
          find(cssSelector("#jib .breadcrumb a")).value.text mustBe "johnsmith" 
       }
    }

    "create wishlist" in {
       click on textField("title")
       textField("title").value = "My Xmas list"
       submit()
       eventually {
         pageTitle mustBe "Wish My Xmas list"
       }
    }

    "add wish" in {       
       click on textField("title")
       textField("title").value = "Blue woolly socks"
       submit()
       eventually {
         pageTitle mustBe "Wish My Xmas list"
         find(cssSelector(".alert-success")).value.text must include ("Wish added") 
         find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include ("Blue woolly socks")
       }
    }

    "log out john" in {      
      click on linkText("log out")
      eventually {
          pageTitle mustBe "Wish" 
          find(cssSelector(".alert-info")).value.text must include ("You have been logged out") 
      }
    }
  }
  
  "Can reserve a wish" must {

    "register and log in another user" in {
       go to s"http://localhost:$port/register.html"
       click on textField("fullname")
       textField("fullname").value = "Sue Smith"
       textField("username").value = "suesmith"
       textField("email").value = "suesmith@example.com"
       pwdField("password").value = "simsalabim"
       pwdField("confirm").value = "simsalabim"
       submit()
       eventually { 
          pageTitle mustBe "Wish" 
          find(cssSelector(".alert-success")).value.text must include ("Welcome, you have successfully registered.") 
       }
       go to s"http://localhost:$port/test-only/recipient/suesmith/verify/find"
       eventually { 
          find(cssSelector(".alert-success")).value.text must include ("Email address verified. Please log in")           
       }
       click on textField("username")
       textField("username").value = "suesmith"
       pwdField("password").value = "simsalabim"
       submit()
       eventually { 
          pageTitle mustBe "Wish" 
          find(cssSelector(".alert-info")).value.text must include ("You have logged in") 
          find(cssSelector("#jib .breadcrumb a")).value.text mustBe "suesmith"
       }
    }

      "find other user's wishlist" in {
         click on textField("term")
         textField("term").value = "Xmas"
         submit()
         eventually {
            pageTitle mustBe "Wish search"
            find(linkText("My Xmas list"))
            find(linkText("johnsmith")) 
         }
      }

      "list wishes" in {
         click on linkText("My Xmas list")
         eventually {
            pageTitle mustBe "Wish My Xmas list"
         find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include ("Blue woolly socks")
         find(cssSelector(".wish-row reserved")) mustBe None
         }
      }

      "find wish" in {
         click on find(cssSelector("#wishlist-wishes #wish-list a")).value
         eventually {
            find(cssSelector(".wish-modal .reserve-button")).value.text mustBe "reserve"
         }
      }

      "reserve wish" in {         
         click on find(cssSelector(".reserve-button")).value
         eventually {
            find(cssSelector(".alert-success")).value.text must include ("Wish reserved")
            find(cssSelector(".wish-row .reserved")).value.text mustBe "reserved"
         }
      }

      "logout sue" in { 
         go to s"http://localhost:$port/logout"
         eventually {
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have been logged out") 
         }
      }
   }
   
   "Can nor reserve a reserved wish" must {

      "register and log in third user" in { 
         go to s"http://localhost:$port/register.html"
         click on textField("fullname")
         textField("fullname").value = "Carl Smith"
         textField("username").value = "carlsmith"
         textField("email").value = "carlsmith@example.com"
         pwdField("password").value = "simsalabim"
         pwdField("confirm").value = "simsalabim"
         submit()
         eventually { 
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-success")).value.text must include ("Welcome, you have successfully registered.") 
         }
         go to s"http://localhost:$port/test-only/recipient/carlsmith/verify/find"
         eventually { 
            find(cssSelector(".alert-success")).value.text must include ("Email address verified. Please log in")           
         }
         click on textField("username")
         textField("username").value = "carlsmith"
         pwdField("password").value = "simsalabim"
         submit()
         eventually { 
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have logged in") 
            find(cssSelector("#jib .breadcrumb a")).value.text mustBe "carlsmith"
         }
      }

      "find wish list" in {         
         click on textField("term")
         textField("term").value = "Xmas"
         submit()
         eventually {
            pageTitle mustBe "Wish search"
            find(linkText("My Xmas list"))
            find(linkText("johnsmith")) 
         }
         click on linkText("My Xmas list")
         eventually {
            pageTitle mustBe "Wish My Xmas list"
         }
      }

      "list wish as reserved" in {         
         find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include("Blue woolly socks")
         find(cssSelector(".wish-row .reserved")).value.text mustBe "reserved"
         click on find(cssSelector("#wishlist-wishes #wish-list a")).value
         eventually {
            find(cssSelector(".modal-header h3")).value.text must include ("Blue woolly socks") 
            find(cssSelector(".reserve-button")) mustBe None
         }
      }

      "logout carl" in { 
         go to s"http://localhost:$port/logout"
         eventually {
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have been logged out") 
         }
      }
   }
   
   "Can unreserve a wish" must {
       
      "login wish reserver" in { 
         go to s"http://localhost:$port/login.html"
         click on textField("username")
         textField("username").value = "suesmith"
         pwdField("password").value = "simsalabim"
         submit()
         eventually { 
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have logged in") 
            find(cssSelector("#jib .breadcrumb a")).value.text mustBe "suesmith"
         }
      }

      "find reserved wish" in {
         click on textField("term")
         textField("term").value = "Xmas"
         submit()
         eventually {
            pageTitle mustBe "Wish search"
            find(linkText("My Xmas list"))
            find(linkText("johnsmith")) 
         }
         click on linkText("My Xmas list")
         eventually {
            pageTitle mustBe "Wish My Xmas list"
            find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include("Blue woolly socks")
            find(cssSelector(".wish-row .reserved")).value.text mustBe "reserved"
         }
         click on find(cssSelector("#wishlist-wishes #wish-list a")).value 
         eventually {
            find(cssSelector(".reserve-button")).value.text mustBe "cancel reservation"
         }
      }

      "unreserve wish" in {
         click on find(cssSelector(".reserve-button")).value
         eventually {
            find(cssSelector(".alert-info")).value.text must include ("Wish reservation cancelled") 
            // find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include("Blue woolly socks")
            // find(cssSelector(".wish-row .reserved")) mustBe None
         }
         click on find(cssSelector("#wishlist-wishes #wish-list a")).value 
         eventually {
            find(cssSelector(".reserve-button")).value.text mustBe "reserve"
         }
      }

      "logout sue" in { 
         go to s"http://localhost:$port/logout"
         eventually {
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have been logged out") 
         }
      }
   }

   "Can delete a wish" must {

      "login wish recipient" in { 
         go to s"http://localhost:$port/login.html"
         click on textField("username")
         textField("username").value = "johnsmith"
         pwdField("password").value = "simsalabim"
         submit()
         eventually { 
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have logged in") 
            find(cssSelector("#jib .breadcrumb a")).value.text mustBe "johnsmith"
         }
      }

      "find wish" in {
         click on find(cssSelector("#wishlist-list a")).value 
         eventually {
            pageTitle mustBe "Wish My Xmas list"
            find(cssSelector("#wishlist-wishes #wish-list a")).value.text must include("Blue woolly socks")
         }
         click on find(cssSelector("#wishlist-wishes #wish-list a")).value 
         eventually {
            find(cssSelector(".wish-edit-button")).value.text mustBe "edit wish"            
         }
      }

      "delete wish" in {
         click on find(cssSelector(".wish-edit-button")).value
         eventually {
            find(cssSelector(".wish-delete-button")).value.text mustBe "delete wish"
         }
         click on find(cssSelector(".wish-delete-button")).value
         eventually {
            find(cssSelector(".alert-warning")).value.text must include ("Wish deleted")
             find(cssSelector("#wishlist-wishes #wish-list a")) mustBe None
         }
      }

      "logout john" in { 
         go to s"http://localhost:$port/logout"
         eventually {
            pageTitle mustBe "Wish" 
            find(cssSelector(".alert-info")).value.text must include ("You have been logged out") 
         }
      }
   }
}
