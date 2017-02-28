rcrdit records TV programs from TV tuners
-----------------------------------------

I wanted an alternative to [tvheadend](https://tvheadend.org/) and [mythtv](https://www.mythtv.org/) that didn't have a 
million features and instead was great at one single job:

1. Recording scheduled shows from TV tuners.

Profiles and autorecs are set up in the database, use database.sql to create the correct structure.  Add as many autorecs
as you wish.

Configure your TV tuner(s) by editing rcrdit.cfg.example.xml, compile the project with `mvn clean package` then run the
project with `java -jar target/rcrdit.jar`

rcrdit.ics is generated on every schedule import which when imported to a calendar program like thunderbird can easily
show which shows will be recorded when and which will be skipped depeding on priority.

A web interface is coming soon.