package rs.ac.bg.etf.players;

import rs.ac.bg.etf.players.Player;
import java.lang.Math;
import java.util.ArrayList;
import java.util.List;

public class Hodjorbeggar extends Player {

	//List<Move> opponentMoves;
	List<Move> myMoves = new ArrayList<Player.Move>();
	
	static final double lambda = 0.2;
	static final int batchSizeForLearning = 10;
	static final int numberOfOpponentsMovesConsidered = 5;
	
	int countInBatch = 0;
	int countGlobal = 0;
	
	boolean[] exceptionalPlayer = {
			false, //0. Goody
			false, //1. Baddy
			false, //2. CopyCat
			false, //3. Forgiver
			false, //4. Avenger
	};
	
	public Hodjorbeggar() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Move getNextMove() {
		if(isExceptionalOpponent())
			return getExceptionalTacticsMove();
		
		Move toMake = Move.DONTPUTCOINS;
		
		
		
		return finalizeMove(toMake);
	}

	private Move finalizeMove(Move made) {
		return made;
	}

	@Override
	public void addOpponentMove(Move move) {
		super.addOpponentMove(move);
		++countGlobal;
		if(++countInBatch>=batchSizeForLearning) {
			//restudyOpponent();
			countInBatch = 0;
		}
	}
	
	private Move getExceptionalTacticsMove() {
		return Move.DONTPUTCOINS;
	}

	private boolean isExceptionalOpponent() {
		return false;
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
		// TODO Auto-generated method stub
		super.resetPlayerState();
	}

	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}
}
