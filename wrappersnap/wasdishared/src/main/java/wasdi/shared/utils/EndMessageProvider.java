/**
 * Created by Cristiano Nattero on 2019-02-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author c.nattero
 *
 */
public class EndMessageProvider {

	private static final List<String> s_asGoodEndMessagges;
	private static final List<String> s_asBadEndMessagges;
	
	static {
		System.out.println("EndMessageProvider static constructor");
		
		String sPrefix =  "----WASDI:";
		String sSuffix = "----";
		
		List<String> asTmpGoodMessages = new ArrayList<>();
		
		asTmpGoodMessages.add(sPrefix + "Operation Done" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Look behind you, a Three-Headed Monster!" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Mission Completed" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "So you want to be a pirate, eh? You look more like a flooring inspector" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Use the force Luke" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "one small step for man, one giant leap for mankind" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "I love it when a plan comes together" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Follow the white Rabbit" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Don't panic" + sSuffix);
		
		s_asGoodEndMessagges = Collections.unmodifiableList(asTmpGoodMessages);
		
		
		List<String> asTmpBadMessages = new ArrayList<>();
		asTmpBadMessages.add(sPrefix + "Not Good" + sSuffix);
		asTmpBadMessages.add(sPrefix + "I have a bad feeling about this" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Not so fast" + sSuffix);
		asTmpBadMessages.add(sPrefix + "A bad beginning makes a bad ending" + sSuffix);
		asTmpBadMessages.add(sPrefix + "What Could Possibly Go Wrong?" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Patience young grasshopper" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Success is always less funny than failure" + sSuffix);
		
		s_asBadEndMessagges = Collections.unmodifiableList(asTmpBadMessages);
		
	}
	
	private String getRandom(List<String> asList) {
		String sTmp = asList.get(((int)(Math.random()*100000))%asList.size()); 
		return sTmp;
	}
	
	public String getGood() {
		return getRandom(s_asGoodEndMessagges);
	}
	
	public String getBad() {
		return getRandom(s_asBadEndMessagges);
	}
}
