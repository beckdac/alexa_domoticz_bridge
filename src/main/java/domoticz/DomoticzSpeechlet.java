package domoticz;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.lang.StringUtils;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.slu.Slot;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

public class DomoticzSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(DomoticzSpeechlet.class);

    /**
     * The key to get the item from the intent.
     */
    private static final String SWITCH_SLOT = "Switch";
    private static final String STATE_SLOT = "State";
    private static final String TEMPERATURE_SLOT = "Temperature";
    private static final String CHANGE_SLOT = "Change";
    private static final String TEMPSENSOR_SLOT = "TempSensor";

    private static final String DOMOTICZ_LIGHT_LIST_URL = "http://domoticz.lan:8080/json.htm?type=devices&filter=light&used=true&order=Name";
    private static final String DOMOTICZ_TEMP_LIST_URL = "http://domoticz.lan:8080/json.htm?type=devices&filter=temp&used=true&order=Name";
    private static final String DOMOTICZ_WEATHER_LIST_URL = "http://domoticz.lan:8080/json.htm?type=devices&filter=weather&used=true&order=Name";
    private static final String DOMOTICZ_UTILITY_LIST_URL = "http://domoticz.lan:8080/json.htm?type=devices&filter=utility&used=true&order=Name";
    private static final String DOMOTICZ_SWITCH_URL = "http://domoticz.lan:8080/json.htm?type=command&param=switchlight&idx=%s&switchcmd=%s";

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        String speechOutput =
		"The house is here to help. What can I help you with?";
        // If the user either does not reply to the welcome message or says
        // something that is not understood, they will be prompted again with this text.
        String repromptText = "For instructions on what you can say, please say help me.";

        // Here we are prompting the user for input
        return newAskResponse(speechOutput, repromptText);
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("SwitchIntent".equals(intentName)) {
            return getSwitch(intent);
        } else if ("TemperatureIntent".equals(intentName)) {
            return getTemperature(intent);
        } else if ("ThermostatIntent".equals(intentName)) {
            return getThermostat(intent);
        } else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelp();
        } else if ("AMAZON.StopIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else if ("AMAZON.CancelIntent".equals(intentName)) {
            PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
            outputSpeech.setText("Goodbye");

            return SpeechletResponse.newTellResponse(outputSpeech);
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        // any cleanup logic goes here
    }

    /**
     * Creates a {@code SpeechletResponse} for the SwitchIntent.
     *
     * @param intent
     *            intent for the request
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getSwitch(Intent intent) {
	Slot switchSlot = intent.getSlot(SWITCH_SLOT);
	Slot stateSlot = intent.getSlot(STATE_SLOT);

	if (switchSlot != null && switchSlot.getValue() != null) {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		SimpleCard card = new SimpleCard();
		String switchName = switchSlot.getValue();
		System.out.println(switchName);
		String outputSpeechString;
		ReadContext ctx = JsonPath.parse(queryDomoticz(DOMOTICZ_LIGHT_LIST_URL));
		System.out.println("looking for switch: " + switchName);
		List<String> statusValue = ctx.read("$.result[?(@.Name =~ /" + switchName + "/i)].Status");
		if (statusValue.size() != 1) {
			outputSpeechString = "I'm sorry, I can't find a switch with that name.  You can ask me for a list of switches.";
			card.setTitle("Switch unknown");
			card.setContent(outputSpeechString);
			return SpeechletResponse.newTellResponse(outputSpeech, card);
		}
		if (stateSlot != null && stateSlot.getValue() != null) {
try {
			String stateName = stateSlot.getValue();
			List<String> idxValue = ctx.read("$.result[?(@.Name =~ /" + switchName + "/i)].idx");
			outputSpeechString = "Turning the " + switchName;
			String switchURL = null;
			if ("on".equals(stateName)) {
				outputSpeechString += " on";
				stateName = "On";
				switchURL = String.format(DOMOTICZ_SWITCH_URL, idxValue.get(0), stateName);
			} else if ("off".equals(stateName)) {
				outputSpeechString += " off";
				stateName = "Off";
				switchURL = String.format(DOMOTICZ_SWITCH_URL, idxValue.get(0), stateName);
			} else {
				// unknown state
				System.out.println('|' + stateName + '|');
				outputSpeechString = "I don't know what state you mean, on or off.";
			}
			if (switchURL != null) {
				System.out.println(switchURL);
				queryDomoticz(switchURL);
			}
			outputSpeech.setText(outputSpeechString);
			card.setTitle("State of" + switchName);
			card.setContent(stateName);
} catch (Exception e) {
	System.err.println("Caught Exception: " + e.getMessage());
}
			return SpeechletResponse.newTellResponse(outputSpeech, card);
		} else {
			// no state value provided, report the state
			if (switchName.substring(switchName.length() - 1).equals("s"))
				outputSpeechString = switchName + " are " + statusValue.get(0);
			else
				outputSpeechString = switchName + " is " + statusValue.get(0);
			outputSpeech.setText(outputSpeechString);
			card.setTitle("State of " + switchName);
			card.setContent(outputSpeechString);
			return SpeechletResponse.newTellResponse(outputSpeech, card);
		}
	} else {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        	SimpleCard card = new SimpleCard();
		String outputSpeechString;
		outputSpeechString = "The house has the following switches: ";
		ReadContext ctx = JsonPath.parse(queryDomoticz(DOMOTICZ_LIGHT_LIST_URL));
		List<String> switchList = ctx.read("$.result..Name");
		String switchListString = StringUtils.join(switchList, ", ");
		outputSpeechString += switchListString;
		outputSpeech.setText(outputSpeechString);
		card.setTitle("Available switches");
		card.setContent(switchListString);
		return SpeechletResponse.newTellResponse(outputSpeech, card);
        }
    }
    private SpeechletResponse getTemperature(Intent intent) {
	Slot tempSensorSlot = intent.getSlot(TEMPSENSOR_SLOT);

	if (tempSensorSlot != null && tempSensorSlot.getValue() != null) {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		SimpleCard card = new SimpleCard();
		String tempSensor = tempSensorSlot.getValue();
		String outputSpeechString;
		ReadContext ctx = JsonPath.parse(queryDomoticz(DOMOTICZ_TEMP_LIST_URL));
		System.out.println("looking for temperature of sensor: " + tempSensor);
		List<String> tempValue = ctx.read("$.result[?(@.Name =~ /" + tempSensor + "/i)].Temp");
		if (tempValue.size() != 1) {
			outputSpeechString = "I'm sorry, I can't find a temperature sensor with that name. You can ask me for a list of temperature sensors.";
		} else {
			outputSpeechString = "The " + tempSensor + " temperature is ";
			String tempString = String.valueOf(tempValue.get(0));
			System.out.println(tempString);
			int temperature = (int)Math.round(Double.parseDouble(tempString));
			System.out.println(temperature);
			outputSpeechString += String.valueOf(temperature) + " degrees";
		}
		outputSpeech.setText(outputSpeechString);
		card.setTitle("Temperature");
		card.setContent(outputSpeechString);
		return SpeechletResponse.newTellResponse(outputSpeech, card);
	} else {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        	SimpleCard card = new SimpleCard();
		String outputSpeechString;
		outputSpeechString = "The house has the following temperature sensors: ";
		ReadContext ctx = JsonPath.parse(queryDomoticz(DOMOTICZ_TEMP_LIST_URL));
		List<String> tempSensorList = ctx.read("$.result..Name");
		String tempListString = StringUtils.join(tempSensorList, ", ");
		outputSpeechString += tempListString;
		outputSpeech.setText(outputSpeechString);
		card.setTitle("Available temperature sensors");
		card.setContent(tempListString);
		return SpeechletResponse.newTellResponse(outputSpeech, card);
	}
    }
    private SpeechletResponse getThermostat(Intent intent) {
	Slot temperatureSlot = intent.getSlot(TEMPERATURE_SLOT);
	Slot changeSlot = intent.getSlot(CHANGE_SLOT);

	if (temperatureSlot != null && temperatureSlot.getValue() != null) {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		SimpleCard card = new SimpleCard();
		int temperature = Integer.parseInt(temperatureSlot.getValue());
		String outputSpeechString;
		outputSpeechString = "Changing thermostat set point to " + temperature + " degrees";
		outputSpeech.setText(outputSpeechString);
		card.setTitle("Thermostat");
		card.setContent(outputSpeechString);
		return SpeechletResponse.newTellResponse(outputSpeech, card);
	} else if (changeSlot != null && changeSlot.getValue() != null) {
		PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
		SimpleCard card = new SimpleCard();
		String change = changeSlot.getValue();
		String outputSpeechString;
		outputSpeechString = "Thermostat " + change;
		outputSpeech.setText(outputSpeechString);
		card.setTitle("Thermostat");
		card.setContent(outputSpeechString);
		return SpeechletResponse.newTellResponse(outputSpeech, card);
	} else {
            // There was no item in the intent so return the help prompt.
            return getHelp();
        }
    }

    /**
     * Creates a {@code SpeechletResponse} for the HelpIntent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelp() {
        String speechOutput =
		"The house is here to help. You can ask a question like, "
			+ "what's the temperature in the sunroom? ... or,"
			+ "tell me to do something like, "
			+ "turn on the fish tank light. ... Now, what can I help you with?";
        String repromptText = "You can say things like, what's the thermostat setting,"
			+ " what light switches are available,"
			+ " or say exit... Now, what can I help you with?";
        return newAskResponse(speechOutput, repromptText);
    }

    /**
     * Wrapper for creating the Ask response. The OutputSpeech and {@link Reprompt} objects are
     * created from the input strings.
     *
     * @param stringOutput
     *            the output to be spoken
     * @param repromptText
     *            the reprompt for if the user doesn't reply or is misunderstood.
     * @return SpeechletResponse the speechlet response
     */
    private SpeechletResponse newAskResponse(String stringOutput, String repromptText) {
        PlainTextOutputSpeech outputSpeech = new PlainTextOutputSpeech();
        outputSpeech.setText(stringOutput);

        PlainTextOutputSpeech repromptOutputSpeech = new PlainTextOutputSpeech();
        repromptOutputSpeech.setText(repromptText);
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(repromptOutputSpeech);

        return SpeechletResponse.newAskResponse(outputSpeech, reprompt);
    }

	private String queryDomoticz(String url) {
	    try {
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		System.out.println("Response Code : " + responseCode);
		
		BufferedReader in = new BufferedReader(
		        new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		return(response.toString());
	    } catch (Exception e) {
		System.err.println("Caught Exception: " + e.getMessage());
		return(e.getMessage());
	    }
	}
}
