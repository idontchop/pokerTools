package pokerTools;

/* TODO:
 * 
 * Optimization:
 * Find any local variables and move to instance.
 * 
 */


/** This class is meant to return a unique integer based on the 1-5 card hand value
	 * of any number of cards passed to it. If you send it a whole deck, you'll get 
	 * the value for a Royal Flush. If you send it 1 duece, you'll get the value of 0
	 * It assumes it will not get duplicates or you know what you are doing in passing
	 * them. If you send Ac Ac Kc Kc Kc, you get a full house.
	 * 
	 * Meant to be used as a static class, since it is only a single integer value that
	 * this class produces
	 * later modifications however might have it returning an array of the cards that 
	 * make the best hand
	 * 
	 * Many of the ideas in this class were taken from Andrew Prock's work:
	 * https://github.com/andrewprock
	 * 
	 */
public class HandValue {
	private static Card[] cards; 	//breaks down the cards we are passed into this array
	private static int spadesMap;	// these ints separate the cards for each suit
	private static int heartsMap;
	private static int diamondsMap;
	private static int clubsMap;
	private static int numSpades, numHearts, numDiamonds, numClubs; // for easy flush finding
	private static long ranksMap;	// This int will count each of the ranks in 3 bit segments
					// For example, 4 3's and 3 5's will turn on bits like so: 011 000 100 000
	private static int singleRanksMap;		//1 bit for each rank
	
	// value multiples -- these are used to set the rankings of high cards
	private static final int[] VALMUL = {1,14,196,2744,38416};
	// hand multiples -- these are used to set the rankings of the hand strength
	// these are set to millions so the 5 card high hand will never overtake one of these
	private static final int RANKMUL = 			1000000; 
	private static final int FLUSHMUL = 		6*RANKMUL;
	private static final int STRAIGHTMUL = 		5*RANKMUL;
	private static final int STRAIGHTFLUSHMUL =	10*RANKMUL; //just cause a Royal should be 10 :D
	private static final int TRIPSMUL = 		4*RANKMUL;
	private static final int BOATMUL = 			7*RANKMUL;
	private static final int QUADSMUL = 		8*RANKMUL;
	private static final int TWOPAIRMUL = 		3*RANKMUL;
	private static final int ONEPAIRMUL = 		2*RANKMUL;
	
	private static int handStringFormat = 0;
	//Sets the method of hand String
	//TODO: do something? This is for later modifications where this class will return an language representation of the 5 card hand.
	void setHandStringFormat(int handStringFormat) {
		HandValue.handStringFormat = handStringFormat;
	}
	//resets all the static
	private static void reset() {
		spadesMap = heartsMap = diamondsMap = clubsMap = numSpades = numHearts = numDiamonds = numClubs =
				singleRanksMap = 0;
		ranksMap = 0L;
	}
	public HandValue() {
		
	}
	
	public static boolean debug = false;
	public static void setDebug(boolean d) {
		debug = d;
	}
	static void debug() {
		System.out.printf("S: %d\nH: %d\nD: %d\nC: %d\n",numSpades, numHearts, numDiamonds, numClubs);
		printBits("Ranks: ",ranksMap);
		System.out.println("Ranks val: " + ranksMap);
		printBits("Sing : ",singleRanksMap);
	}
	
	static void printBits(String name, long b) {
		System.out.printf("%s ", name);
		System.out.print(Long.toString(b,2));
		System.out.println();
	}
		
	/* getHandValue() will be overloaded to allow the sending of hands in different ways
	 * The main way to send the cards will be an array of Card objects
	 * Alternative ways will be a string
	 */
	public static int getHandValue(Card... c) {
		reset();
		cards = c;
		/* debug
		for (Card cc: cards) {
			cc.print();System.out.print(" ");
		} */
		return findHandValue();
	}
	
	public static int getHandValueNoFlush(Card... c) {
		reset();
		cards = c;
		return findHandValueNoFlush();
	}
	
	/* findHandValue() will be run after we have a proper array of Card objects
	 * It will delegate the setting of the bit maps and then the logic to find the 
	 * value which will be returned to the calling method (usually getHandValue())
	 * 
	 */
	static int value=0,testValue=0; //variables placed outside to avoid instantiation
	private static int findHandValue() {
		value=0;testValue=0;
		setMaps();
		if(debug) debug();
		if ((value = isFlush()) > STRAIGHTFLUSHMUL) return value; //if we have a straight flush, we are done
		if ((testValue = isPairToQuads())>value) value = testValue; 
		//next lines might be a touch confusing... it's meant to save cycles, doing the straight calc is not always necessary
		if ((value < STRAIGHTMUL) && ((testValue = isStraight(singleRanksMap)) > 0) && ( (testValue+=STRAIGHTMUL) > value) ) value = testValue; //First test is to save some cycles 
		if (value >= ONEPAIRMUL) return value;
		//if we make it here, we must have a high hand
		if (debug) debug();
		return findHighCardValue(singleRanksMap,5);
	}
	
	/**
	 * Meant to act the same as findHandValue() but this method assumes the caller already knows there are no flushes possible
	 * @return
	 */
	private static int findHandValueNoFlush() {
		value=0;testValue=0;
		setMapsNoFlush();
		if(debug) debug();
		if ((value = isPairToQuads())>BOATMUL) return value; //if we have a boat or better, we are done 
		//next lines might be a touch confusing... it's meant to save cycles, doing the straight calc is not always necessary
		if ((value < STRAIGHTMUL) && ((testValue = isStraight(singleRanksMap)) > 0) && ( (testValue+=STRAIGHTMUL) > value) ) value = testValue; //First test is to save some cycles 
		if (value >= ONEPAIRMUL) return value;
		//if we make it here, we must have a high hand
		if (debug) debug();
		return findHighCardValue(singleRanksMap,5);

	}
	
	public static String getHandString(int value) {
		String handString = "";
		switch ((value/RANKMUL)*RANKMUL) { //strip out any value less than RANKMUL
		case STRAIGHTFLUSHMUL: 
			if (value%RANKMUL == 10) handString = "Royal Flush";
			else {handString += "Straight Flush to the ";
			handString += getStraightString(value); }
		case QUADSMUL:
			
		break;
			
		}
		
		return handString;
	}
	
	private static String getStraightString(int value) {
		switch (value&RANKMUL) { //just strip out the millions, will work for str8flush and straight
		case 10: return "Ace";
		case 9: return "King";
		case 8: return "Queen";
		case 7: return "Jack";
		case 6: return "Ten";
		case 5: return "Nine";
		case 4: return "Eight";
		case 3: return "Seven";
		case 2: return "Six";
		case 1: return "Five";
		default: return "Five";
		}
	}
	
	/* setMaps() uses the values of the array of Cards to set the class Map integers
	 * 
	 */
	
	private static void setMaps() {
		for (int c = 0; c<cards.length; c++) {
			switch (cards[c].getSuit()){
			case 'c':  //clubs
				clubsMap |= cards[c].getSingleRanksMap();
				numClubs++;
				break;
			case 'd': // diamonds
				diamondsMap |= cards[c].getSingleRanksMap();
				numDiamonds++;
				break;
			case 'h': // hearts 
				heartsMap |= cards[c].getSingleRanksMap();
				numHearts++;
				break;
			case 's':  //spades
				spadesMap |= cards[c].getSingleRanksMap();
				numSpades++;
				break;
			}
			ranksMap += cards[c].getRankMap();			
		}
		// set the singles map by using the suits maps
		singleRanksMap = clubsMap | diamondsMap | heartsMap | spadesMap;
		
	}
	
	/**
	 * Does not set the suits map or suits count. 
	 */
	private static void setMapsNoFlush() {
		for (int c = 0; c<cards.length;c++) {
			singleRanksMap |= cards[c].getSingleRanksMap();
			ranksMap += cards[c].getRankMap();
		}
	}
	
	/**
	 * This method accepts a map of single ranks
	 * and the number of cards it is expected to find a value for
	 * numReduce is an optional parameter used by fullhouse and two pair and such to let this method
	 * 	know it should not use the first multiple. the starting multiple is reduce by numReduce
	 * 
	 * Lets try not to use a table
	 * 
	 * This value is either used by itself or added to the value of a bigger hand (flush, kickers)
	 * 
	 * overloaded when we are finding all 5
	 */
	static int hcValue;
	private static int findHighCardValue(long map,int num,int numReduce) {
		hcValue=0;
		//start at the 13th bit and count down, decreasing num, mulIndex determines where in VALMUL we pull
		int mulIndex = VALMUL.length-1-numReduce;
		//assert (numReduce+num > VALMUL.length) : "numReduce blowup";
		for (int c = 12;c>=0 && num>0;c--) {
			if ((map & (1<<c)) != 0) {hcValue += (c+1)*VALMUL[mulIndex]; mulIndex--;num--; }
		}
		return hcValue;
	}
	
	/** See  findHighCardValue(long map,int num,int numReduce) */
	private static int findHighCardValue(long map) {
		return findHighCardValue(map,5,0);
	}
	/** See findHighCardValue(long map,int num,int numReduce) */
	private static int findHighCardValue(long map, int num) {
		return findHighCardValue(map,num,0);
	}

	/* this method can accept any of the single bit maps. The flush method
	 * will pass the suit map to check for straight flush
	 */
	//STRAIGHTMAPS Stores the bitmaps for the 10 possible straights, the position of the straight in the
	//array determines its value
	private static final int[] STRAIGHTMAPS = {4111, 31, 62, 124, 248, 496, 992, 1984, 3968, 7936};
	private static int isStraight(int map) {
		for (int c=STRAIGHTMAPS.length-1;c>=0;c--) { // counting down
			if ((map & STRAIGHTMAPS[c])==STRAIGHTMAPS[c]) return c+1;
		}
		return 0;
	}
	
	/* Unlike isStraight, isFlush will rely on the globals more since it
	 * has to check all suits for the best flush (who knows maybe some day holdem is played with 10 pockets)
	 * ok, either we commit to globals or get rid of them!
	 */
	private static int isFlush() {
		int val = 0; int testValue=0;
		if (numSpades >= 5) {  //diamonds
			if ((testValue = isStraight(spadesMap)) > 0)
				val = testValue + STRAIGHTFLUSHMUL;
			else val = findHighCardValue(spadesMap) + FLUSHMUL;
		}  if (numHearts >= 5) {  //hearts
			if ((testValue = isStraight(heartsMap)) > 0) {
				if ((testValue += STRAIGHTFLUSHMUL) > val) val = testValue;
			} else if ((testValue += FLUSHMUL + findHighCardValue(heartsMap)) > val) val = testValue;
		}  if (numDiamonds >= 5) { //diamonds
			if ((testValue = isStraight(diamondsMap)) > 0) {
				if ((testValue += STRAIGHTFLUSHMUL) > val) val = testValue;
			} else if ((testValue += FLUSHMUL + findHighCardValue(diamondsMap)) > val) val = testValue;
		}  if (numClubs >= 5) { //clubs
			if ((testValue = isStraight(clubsMap)) > 0) {
				if ((testValue += STRAIGHTFLUSHMUL) > val) val = testValue;
			} else if ((testValue += FLUSHMUL + findHighCardValue(clubsMap)) > val) val = testValue;
		}
		return val;  //if no flush exists return value is 0
	}
	
	/* Like isFlush, isPairToQuads will rely on the globals
	 * This will use the ranksMap to determine the best hand from a single pair, 2pair, trips, full hosue, or quads, ignores straights and flushes
	 * Will also set the kickers
	 * Returns the full value
	 * Always returns the best pair to quads hand regardless of flushes etc
	 * Does not return a high hand value, will rely on other method for that (we should never do that calc until we are sure we have to)
	 */
	private static int isPairToQuads () {
		int tempSingleRanksMap = singleRanksMap; //since we may have to modify this to extract kickers
		int topNum = 0; int botNum = 0; //stores the first and second number of cards found (if quads found, topnum =4)
		int topIndex = -1; int botIndex = -1; //stores the position in the ranksMap where the corresponding Num was found
		int testNum=0; //used to test the current position against already assigned topNum/botNum
		int value=0;   //must return the value instead of using global since this method is independant of flushes etc
		// we will never check for a Canadian Full House. Carnival games are not part of this Evaluator (yet)
		/* for loop through the ranksMap
		 * TODO: figure out how to not hard code the bit map 12 and 3 which relies on too much stuff not changing (I don't know... like relies on the deck of cards not changing? lol still seems not real OOPish)
		 * Since we are counting down, we can simply check for finding more of a certain rank (QQQ is always better than KK, but never better than KKK)
		 */
		for (int currentindex = 12;currentindex>=0;currentindex--) {
			testNum = (int)((ranksMap>>(currentindex*3)) & 7L);
			if (testNum > topNum) {
				botNum = topNum; //auto set the bottom
				botIndex = topIndex;
				topNum = testNum;
				topIndex = currentindex;
			} else if (testNum > botNum) {
				botNum = testNum;
				botIndex = currentindex;
			}
			
		} // end index loop
		
		// now that we know the best paired or better hand, we need to remove those indices from the singleranksMap and find the kickers
		if (topNum == 4) { //quads - no need to worry about bot
			tempSingleRanksMap ^= (1<<topIndex); //remove the quads
			value = QUADSMUL + (VALMUL[4]*topIndex) + findHighCardValue(tempSingleRanksMap,1,1); //set to quads multiple and add one kicker
		} else if (topNum == 3) { //boat or trips
			if (botNum > 1) { //boat
				//for boat, no need to modify ranksmap
				value = BOATMUL + (VALMUL[4]*topIndex) + (VALMUL[3]*botIndex);
			} else { //trips
				tempSingleRanksMap ^= (1<<topIndex);
				value = TRIPSMUL + (VALMUL[4]*topIndex) + findHighCardValue(tempSingleRanksMap,2,1); //2 kickers
			}
		} else if (topNum == 2) { //2 pair or 1 pair
			if (botNum > 1) {  //2 pair
				tempSingleRanksMap ^= (1<<topIndex) + (1<<botIndex); // remove both pairs
				value = TWOPAIRMUL + (VALMUL[4]*topIndex) + (VALMUL[3]*botIndex) + findHighCardValue(tempSingleRanksMap,1,2);
			} else {  //1 pair
				tempSingleRanksMap ^= (1<<topIndex);
				value = ONEPAIRMUL + (VALMUL[4]*topIndex) + findHighCardValue(tempSingleRanksMap,3,1); //3 kickers
			}
		} 

		return value;
	}
}
