package rs.ac.bg.etf.players;

import rs.ac.bg.etf.players.Player;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

public class Hodjorbeggar extends Player {

	//List<Move> opponentMoves;
	protected List<Move> myMoves = new ArrayList<Player.Move>();
	
	static final boolean versusMode = true;
	
	static int idGen = 0;
	int id = idGen++;
	static final double lambda = 0.5;
	static final int numberOfOpponentsMovesConsidered = 5;
	static final int batchSizeForLearning = 10;
	static final double allowedErrorRate = 0.2;
	//static final int bigBatchSizeForLearning = 100;
	static final int hardCodedLogicMoveCountTreshold = 3;
	
	static boolean useSacrificialPawns = false;
	static double probabilityToChangeSacrificialPawnTactic = 0.00001;
	
	int countInBatch = 0;
	int countGlobal = 0;
	
	int currentValue = 0;
	
	private boolean expectingForgiverOrCopycat = false;
	private boolean foundSelf = false;
	private Move moveAgainstSelf = Move.PUT1COIN; //Placeholder default value, never used
	
	Random rand = new Random();
	
	boolean[] exceptionalPlayer = {
			false, //0. Goody
			false, //1. Baddy
			false, //2. CopyCat
			false, //3. Forgiver
			false, //4. Avenger
			false, //5. My humble self
			false, //6. Nemesis
	};
	
	public Hodjorbeggar() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Move getNextMove() {
		if(foundSelf)
			return finalizeMove(strategyAgainstSelf());
		
		if( countGlobal <= hardCodedLogicMoveCountTreshold || isExceptionalOpponent() )
			return finalizeMove(hardCodedLogic());
		
		//System.out.print("Something entered heuristics...");
		
		Move toMake;
		
		double receivedValueWeightedSum = IntStream.range(0, numberOfOpponentsMovesConsidered)
			.mapToDouble(i -> {
				if(i>=opponentMoves.size())
					return 0;
				return value(opponentMoves.get(i)) * getWeight(i);
				})
			.reduce( (a,b) -> a+b )
			.getAsDouble();
			
		
		if( receivedValueWeightedSum >= threshold(Move.PUT2COINS) )
			toMake = Move.PUT2COINS;
		else if( receivedValueWeightedSum >= threshold(Move.PUT2COINS) )
			toMake = Move.PUT1COIN;
		else{
			toMake = Move.DONTPUTCOINS;
			if(rand.nextDouble()<0.0005)
				toMake = Move.PUT2COINS;
		};
		
		return finalizeMove(toMake);
	}
	
	private Move hardCodedLogic() {//0 1 2 [3]
		if(countGlobal == 0)
			return Move.PUT1COIN;
		if(countGlobal == 1) {
			if(opponentMoves.get(0) == Move.PUT2COINS ) {
				exceptionalPlayer[0] = true;
				exceptionalPlayer[4] = true;
				return opponentMoves.get(0);
			}
			else if(opponentMoves.get(0) == Move.PUT1COIN ) {
				exceptionalPlayer[2] = true;
				exceptionalPlayer[3] = true;
				exceptionalPlayer[5] = true;
				return Move.DONTPUTCOINS;
			}
			else {
				exceptionalPlayer[1] = true;
				return Move.DONTPUTCOINS;
			}
		}
		if(countGlobal == 2) {
			if(exceptionalPlayer[1])
				return Move.DONTPUTCOINS;
			if(exceptionalPlayer[2] || exceptionalPlayer[3] || exceptionalPlayer[5]) {
				if(opponentMoves.get(0) == Move.DONTPUTCOINS) {
					exceptionalPlayer[3] = false;
					exceptionalPlayer[2] = false;
					foundSelf = true;
					return strategyAgainstSelf();
				}
				else if(opponentMoves.get(0) == Move.PUT1COIN){
					exceptionalPlayer[5] = false;
					expectingForgiverOrCopycat= true;
					return Move.DONTPUTCOINS;
				}
				else {
					exceptionalPlayer[6] = true;
				}
				return Move.PUT2COINS;
			}
			if(exceptionalPlayer[0] || exceptionalPlayer[4]) {
				if(opponentMoves.get(0) == Move.PUT1COIN) {
					exceptionalPlayer[0] = false;
					return Move.PUT1COIN;
				}
				else {
					exceptionalPlayer[4] = false;
					return Move.DONTPUTCOINS;
				}
			}
		}
		
		if( expectingForgiverOrCopycat ) {
			expectingForgiverOrCopycat = false;
			if(opponentMoves.get(0) == Move.DONTPUTCOINS)
				exceptionalPlayer[3] = false;
			else
				exceptionalPlayer[2] = false;
			return Move.PUT2COINS;
		}
		else if( exceptionalPlayer[2] && exceptionalPlayer[3] ) {
			expectingForgiverOrCopycat = true;
			return Move.DONTPUTCOINS;
		}
		
		//Moves 3+
		if(exceptionalPlayer[0] || exceptionalPlayer[1]) {
			if(exceptionalPlayer[0] && opponentMoves.get(0)!=Move.PUT2COINS) {
				exceptionalPlayer[0] = false;
				return opponentMoves.get(0);
			}
			if(exceptionalPlayer[1] && opponentMoves.get(0)!=Move.DONTPUTCOINS) {
				exceptionalPlayer[1] = false;
				return opponentMoves.get(0);
			}
			
			return Move.DONTPUTCOINS;
		}
		if(exceptionalPlayer[2]) {
			if(opponentMoves.get(0)!=myMoves.get(1)) {
				exceptionalPlayer[2] = false;
				return opponentMoves.get(0);
			}
			return countGlobal%2==0 ? Move.PUT2COINS : opponentMoves.get(0);
		}
		if(exceptionalPlayer[3]) {
			if(countGlobal > 4 && opponentMoves.get(0) == Move.DONTPUTCOINS) {
				exceptionalPlayer[3] = false;
				return opponentMoves.get(0);
			}
			return countGlobal%2==0 ? Move.DONTPUTCOINS : Move.PUT2COINS;
		}
		if(exceptionalPlayer[4]) {
			return Move.DONTPUTCOINS;// We want him out, no matter what.
		}
		
		return opponentMoves.get(0); //Return copycat against intruders.
	}

	private Move max(Move move, Move move2) {
		if(move.compareTo(move2)<0) return move2;
		else return move;
	}

	private Move min(Move move, Move move2) {
		if(move.compareTo(move2)<0) return move;
		else return move2;
	}

	private int delayerHelper = 0;
	private Move strategyAgainstSelf() {		
		if(!useSacrificialPawns) {
			if(++delayerHelper>1) {
				if(opponentMoves.get(0) != Move.PUT2COINS)
					exceptionalPlayer[5] = false;
				return opponentMoves.get(0);
			}
			
			return Move.PUT2COINS;
		}
		
		if(moveAgainstSelf!=Move.PUT1COIN) {
			if( ++delayerHelper>1 ) {
				if(opponentMoves.get(0) == Move.PUT1COIN || opponentMoves.get(0) == myMoves.get(0)) {
					exceptionalPlayer[5] = false;
					return opponentMoves.get(0);
				}
			}
			return moveAgainstSelf;
		}
		
		if(myMoves.get(0) == Move.PUT2COINS && opponentMoves.get(0) == Move.DONTPUTCOINS)
			moveAgainstSelf = Move.PUT2COINS;
		else if(myMoves.get(0) == Move.DONTPUTCOINS && opponentMoves.get(0) == Move.PUT2COINS)
			moveAgainstSelf = Move.DONTPUTCOINS;
		else return getAlternatingBoolean() ? Move.PUT2COINS : Move.DONTPUTCOINS;
		
		return moveAgainstSelf;
	}
	
	static boolean currentAlternatingBoolean = false;
	static boolean getAlternatingBoolean() {
		currentAlternatingBoolean = !currentAlternatingBoolean;
		return currentAlternatingBoolean;
	}

	private double threshold(Move m) {
		switch( m ) {
		case PUT2COINS:
			//return numberOfOpponentsMovesConsidered * (value(Move.PUT2COINS)+value(Move.PUT1COIN))/2;
			return 12;
		case PUT1COIN:
			//return numberOfOpponentsMovesConsidered * value(Move.PUT1COIN)/2;
			return 5;
		default:
			return 0;
		}
	}

	private Move finalizeMove(Move made) {
		myMoves.add(0, made);
		return made;
	}

	@Override
	public void addOpponentMove(Move move) {
		super.addOpponentMove(move);
		
		Move addedMove = opponentMoves.remove(opponentMoves.size()-1);
		opponentMoves.add(0, addedMove);
		
		++countGlobal;
		if( !isExceptionalOpponent() ) {
			restudyOpponent();
			countInBatch = 0;
		}
	}

	private void restudyOpponent() {
		//System.out.println("Studying..");
		double requiredPrecision = 1 - allowedErrorRate;
		
		int numOfOpponentsMoves = opponentMoves.size();
		
		if( (double)opponentMoves.parallelStream()
				.filter( m -> m == Move.PUT2COINS)
				.count() >= requiredPrecision*numOfOpponentsMoves )
			exceptionalPlayer[0] = true;
		
		if( (double)opponentMoves.parallelStream()
				.filter( m -> m == Move.DONTPUTCOINS)
				.count() >= requiredPrecision*numOfOpponentsMoves )
			exceptionalPlayer[1] = true;
		
		long matchingMoves = IntStream.range(0, opponentMoves.size())
			.filter(i -> {
				if(i+1 >= myMoves.size())
					return false;
				return opponentMoves.get(i) == myMoves.get(i+1);
			}).count();
		if( matchingMoves >= requiredPrecision*(numOfOpponentsMoves-1) )
			exceptionalPlayer[2] = true;
		
		matchingMoves = IntStream.range(0, opponentMoves.size())
				.filter(i -> {
					if(i+1 >= myMoves.size())
						return false;
					if(myMoves.get(i+1) == Move.DONTPUTCOINS)
						return true;
					return opponentMoves.get(i) == myMoves.get(i+1);
				}).count();
			if( matchingMoves >= requiredPrecision*(numOfOpponentsMoves-1) )
				exceptionalPlayer[3] = true;
			
		
		
	}

	private boolean isExceptionalOpponent() {
		for( boolean e : exceptionalPlayer )
			if(e) return e;
		return false;
	}
	
	private double value(Move m) {
		if(m==Move.PUT2COINS)
			return 7;
		if(m==Move.PUT1COIN)
			return 3;
		return 0;
	}
	
	/**
	 * @param index Indeks prethodnog poteza, pocinje od 0.
	 * @return Tezina odgovarajuceg rezultata na odluku.
	 */
	private static double getWeight(int index) {
		return Math.pow(Math.E, -lambda*index);
	}
	
	@Override
	public void resetPlayerState() {
		super.resetPlayerState();
		myMoves.clear();
		countGlobal = 0;
		countInBatch = 0;
		currentValue = 0;
		
		delayerHelper = 0;
		
		expectingForgiverOrCopycat = false;
		foundSelf = false;
		moveAgainstSelf = Move.PUT1COIN;
		
		for(int i = 0; i < exceptionalPlayer.length; ++i) {
			exceptionalPlayer[i] = false;
		}
		
		if(rand.nextDouble() < probabilityToChangeSacrificialPawnTactic) {
			useSacrificialPawns = !useSacrificialPawns;
			probabilityToChangeSacrificialPawnTactic = 0;
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
