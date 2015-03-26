package com.tt.droidplay;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AirPlayUtils {

	// List of supported image transition effects
	private static List<String> transitions = new ArrayList<String>();
	
	// Map of descriptions for image transition effects
	private static Map<String, String> transitionDescriptions = new HashMap<String ,String>();

	static {
		// transition codes
		transitions.add("None");
		transitions.add("Random");
		transitions.add("Dissolve");
		transitions.add("SlideLeft");
		transitions.add("SlideRight");
		
		// transition descriptions
		transitionDescriptions.put("None", "None");
		transitionDescriptions.put("Random", "Random");
		transitionDescriptions.put("Dissolve", "Dissolve");
		transitionDescriptions.put("SlideLeft", "Slide left");
		transitionDescriptions.put("SlideRight", "Slide right");
	}
	
	/**
	 * Return the list of supported image transition effects.
	 * 
	 * @return The list of effects (used in HTTP headers).
	 */
	public static List<String> getTransitions() {
		return (transitions);
	}
	
	/**
	 * Return the list of descriptions for image transition effects.
	 * 
	 * @return The list of descriptions (used in the UI).
	 */
	public static List<String> getTransitionDescriptions() {
		List<String> descriptions = new ArrayList<String>();
		for (String code : transitions) {
			descriptions.add(transitionDescriptions.get(code));
		}
		return (descriptions);
	}
	
	/**
	 * Return the image transition effect at a specific position (used in the UI settings drop-down list).
	 * 
	 * @param position The position.
	 * @return The image transition effect
	 */
	public static String getTransition(int position) {
		return (transitions.get(position));
	}
	
}
