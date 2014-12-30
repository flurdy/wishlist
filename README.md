* Website: http://wishlist.sourceforge.net
* Project: http://sourceforge.net/projects/wishlist
* Source: http://github.com/flurdy/wishlist
* Tasks: http://pivotaltracker.com/projects/101944
* Demo: http://wish.flurdy.com


Summary:
---------
A place to store and share wish lists.
* multiple cross referenced lists
* private / public / shielded from recipient(s)
* wish item reservation
* wish item collaboration
* integrate with gift registry
* integrate with shopping plans

![Build status](https://travis-ci.org/flurdy/wishlist.png)




Install
-----------

Install Scala, SBT and the Play Framework 2.x

http://www.playframework.org/documentation/2.2.x/Installing

or on OSX `brew install play`

**Play! 2.0 + Scala + Heroku by flurdy**
http://flurdy.com/docs/herokuplay/play2.html


Run
-----

Simple start Play with `play`
and then perhaps `; clean ; compile ; ~run`
to clean the folder, compile all classes and start the web application.
The `~` will automatically detect and recompile any changes.


Configuration
-------------

Default values let you run the application locally.

However for staging and production deploys you want to override certain values.
Such as
* Web application port
* Database details
* Email server details
* Default email addresses
* Hostname of domain
* Analytics Id

The  application is set up to run on Heroku http://www.heroku.com as an example.
The **Procfile** is Heroku specific which configures the app via **conf/heroku.conf**.
In that file a number of environment properties is configured for the the application.

The application is also configured to be run with Docker and Fig.




