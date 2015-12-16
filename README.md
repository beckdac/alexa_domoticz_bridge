# alexa_domoticz_bridge
Amazon Echo Alexa to domoticz bridge

[_Demo on youtube._](https://www.youtube.com/watch?v=BCLQecMM_tg)

# Instructions

### Server side
* _Where?_ This code will run on your domoticz box.  It needs to have the SSL HTTP port redirected from your public facing firewall. You have been warned, but [read up on how the web service works](https://developer.amazon.com/public/solutions/alexa/alexa-skills-kit/docs/developing-an-alexa-skill-as-a-web-service) and what precautions are taken to verify Amazon as the source of requests.
* _Overlay onto the Amazon Alexa Skills Kit SDK & Samples_.  Start by downloading and unpacking the Amazon Alexa Skills Kit SDK & Samples zip file. More information about the Amazon Alexa Skills Kit SDK here: [Amazon Apps and Services Developer Portal](https://developer.amazon.com/appsandservices/solutions/alexa/alexa-skills-kit/).  Replace files in the SDK with files from this repository. 
* _Rebuild the source._  Build the source with the ./rebuild script.
* _Run the code._ Run with the ./run script.
### Amazon Echo 
* _Create a new Alexa Skill on Amazon's Developer site_.  If you haven't done this before, **stop** and setup at least one of the demos in the Skills Kit SDK and make sure it works with the server configuration you intend to use first.
* Put in the contents of the speechAssets folder into the approriate boxes on the skill's Interaction Model page.
* _Enable the Skill & Test_

# Contents
This includes a modified pom.xml and Launcher.java to support the skills.  The pom includes necessary dependencies, e.g. javax.json. Launcher.java calls the speechlet
