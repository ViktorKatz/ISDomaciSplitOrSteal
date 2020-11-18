package rs.ac.bg.etf.players;

import rs.ac.bg.etf.players.Player;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;
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
	static final int bigBatchSizeForLearning = 100;
	static final int hardCodedLogicMoveCountTreshold = 3;
	
	int countInBatch = 0;
	int countGlobal = 0;
	
	int currentValue = 0;
	
	private boolean expectingForgiverOrCopycat = false;
	private boolean foundSelf = false;
	private Move moveAgainstSelf = Move.PUT1COIN; //Placeholder default value, never used
	
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
		
		System.out.print("Something entered heuristics...");
		
		Move toMake;
		
		double receivedValueWeightedSum = IntStream.range(0, numberOfOpponentsMovesConsidered)
			.mapToDouble(i -> value(opponentMoves.get(i)) * getWeight(i))
			.reduce( (a,b) -> a+b )
			.getAsDouble();
			
		
		if( receivedValueWeightedSum >= threshold(Move.PUT2COINS) )
			toMake = Move.PUT2COINS;
		else if( receivedValueWeightedSum >= threshold(Move.PUT2COINS) )
			toMake = Move.PUT1COIN;
		else toMake = Move.DONTPUTCOINS;
		
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
		if(exceptionalPlayer[0] || exceptionalPlayer[1])
			return Move.DONTPUTCOINS;
		if(exceptionalPlayer[2])
			return countGlobal%2==0 ? Move.PUT2COINS : opponentMoves.get(0);
		if(exceptionalPlayer[3])
			return countGlobal%2==0 ? Move.DONTPUTCOINS : Move.PUT2COINS;
		if(exceptionalPlayer[4])
			return Move.DONTPUTCOINS; //opponentMoves.get(0);
		
		return opponentMoves.get(0); //Return copycat against intruders.
	}

	private Move strategyAgainstSelf() {
		return Move.PUT2COINS;
		/*
		if(moveAgainstSelf!=Move.PUT1COIN)
			return moveAgainstSelf;
		
		if(myMoves.get(0) == Move.PUT2COINS && opponentMoves.get(0) == Move.DONTPUTCOINS)
			moveAgainstSelf = Move.PUT2COINS;
		else if(myMoves.get(0) == Move.DONTPUTCOINS && opponentMoves.get(0) == Move.PUT2COINS)
			moveAgainstSelf = Move.DONTPUTCOINS;
		else return getAlternatingBoolean() ? Move.PUT2COINS : Move.DONTPUTCOINS;
		
		return moveAgainstSelf;*/
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
		if(++countInBatch>=batchSizeForLearning) {
			//restudyOpponent();
			countInBatch = 0;
		}
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
		
		expectingForgiverOrCopycat = false;
		foundSelf = false;
		moveAgainstSelf = Move.PUT1COIN;
		
		for(int i = 0; i < exceptionalPlayer.length; ++i) {
			exceptionalPlayer[i] = false;
		}
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
