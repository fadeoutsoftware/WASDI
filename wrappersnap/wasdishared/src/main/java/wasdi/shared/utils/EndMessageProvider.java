/**
 * Created by Cristiano Nattero on 2019-02-25
 * 
 * Fadeout software
 *
 */
package wasdi.shared.utils;

import java.security.SecureRandom;
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
		
		String sPrefix =  "----WASDI: ";
		String sSuffix = " ----";
		
		List<String> asTmpGoodMessages = new ArrayList<>();
		
		asTmpGoodMessages.add(sPrefix + "Operation Done" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Look behind you, a Three-Headed Monster!" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Mission Completed" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "So you want to be a pirate, eh? You look more like a flooring inspector" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Use the force Luke" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "One small step for man, one giant leap for mankind" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "I love it when a plan comes together" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Follow the white Rabbit" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Don't panic" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "If I'm not back in five minutes, just wait longer" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "I find your lack of faith disturbing." + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Try not. Do, or do not. There is no try." + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Goonies never say die." + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Nothing shocks me I'm a scientist." + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Are you telling me you built a time machine... out of a DeLorean?" + sSuffix);
		asTmpGoodMessages.add(sPrefix + "Yeah, well. The Dude abides." + sSuffix);
		
		s_asGoodEndMessagges = Collections.unmodifiableList(asTmpGoodMessages);
		
		
		List<String> asTmpBadMessages = new ArrayList<>();
		asTmpBadMessages.add(sPrefix + "Not Good" + sSuffix);
		asTmpBadMessages.add(sPrefix + "I have a bad feeling about this" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Not so fast" + sSuffix);
		asTmpBadMessages.add(sPrefix + "A bad beginning makes a bad ending" + sSuffix);
		asTmpBadMessages.add(sPrefix + "What Could Possibly Go Wrong?" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Patience young grasshopper" + sSuffix);
		asTmpBadMessages.add(sPrefix + "Success is always less funny than failure" + sSuffix);
		asTmpBadMessages.add(sPrefix + "I'm sorry, Dave. I'm afraid I can't do that." + sSuffix);
		asTmpBadMessages.add(sPrefix + "it would be a Twinkie thirty-five feet long, weighing approximately six hundred pounds." + sSuffix);
		asTmpBadMessages.add(sPrefix + "Worst. Episode. Ever." + sSuffix);
		asTmpBadMessages.add(sPrefix + "If we knew what it was we were doing, it would not be called research, would it?" + sSuffix);
		asTmpBadMessages.add(sPrefix + "These aren't the droids you're looking for." + sSuffix);
		asTmpBadMessages.add(sPrefix + "He slimed me!" + sSuffix);
		
		
		
		s_asBadEndMessagges = Collections.unmodifiableList(asTmpBadMessages);
		
	}
	
	private String getRandom(List<String> asList) {
		String sTmp = asList.get(new SecureRandom().nextInt(asList.size())); 
		return sTmp;
	}
	
	public String getGood() {
		return getRandom(s_asGoodEndMessagges);
	}
	
	public String getBad() {
		return getRandom(s_asBadEndMessagges);
	}
}
