import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/// Represents the state of a chess game
class ChessState
{
	public static final int MAX_PIECE_MOVES = 27;
	public static final int None = 0;
	public static final int Pawn = 1;
	public static final int Rook = 2;
	public static final int Knight = 3;
	public static final int Bishop = 4;
	public static final int Queen = 5;
	public static final int King = 6;
	public static final int PieceMask = 7;
	public static final int WhiteMask = 8;
	public static final int AllMask = 15;

	int[] m_rows;
	boolean kingCaptured = false;
	private final static Random rand = new Random();
	private Scanner sc = new Scanner(System.in);

	ChessState()
	{
		m_rows = new int[8];
		resetBoard();
	}

	ChessState(ChessState that)
	{
		m_rows = new int[8];
		for (int i = 0; i < 8; i++)
		{ this.m_rows[i] = that.m_rows[i]; }
	}

	int getPiece(int col, int row)
	{
		return (m_rows[row] >> (4 * col)) & PieceMask;
	}

	boolean isWhite(int col, int row)
	{
		return (((m_rows[row] >> (4 * col)) & WhiteMask) > 0 ? true : false);
	}

	/// Sets the piece at location (col, row). If piece is None, then it doesn't
	/// matter what the value of white is.
	void setPiece(int col, int row, int piece, boolean white)
	{
		m_rows[row] &= (~ (AllMask << (4 * col)));
		m_rows[row] |= ((piece | (white ? WhiteMask : 0)) << (4 * col));
	}

	/// Sets up the board for a new game
	void resetBoard()
	{
		setPiece(0, 0, Rook, true);
		setPiece(1, 0, Knight, true);
		setPiece(2, 0, Bishop, true);
		setPiece(3, 0, Queen, true);
		setPiece(4, 0, King, true);
		setPiece(5, 0, Bishop, true);
		setPiece(6, 0, Knight, true);
		setPiece(7, 0, Rook, true);
		for (int i = 0; i < 8; i++)
		{ setPiece(i, 1, Pawn, true); }
		for (int j = 2; j < 6; j++)
		{
			for (int i = 0; i < 8; i++)
			{ setPiece(i, j, None, false); }
		}
		for (int i = 0; i < 8; i++)
		{ setPiece(i, 6, Pawn, false); }
		setPiece(0, 7, Rook, false);
		setPiece(1, 7, Knight, false);
		setPiece(2, 7, Bishop, false);
		setPiece(3, 7, Queen, false);
		setPiece(4, 7, King, false);
		setPiece(5, 7, Bishop, false);
		setPiece(6, 7, Knight, false);
		setPiece(7, 7, Rook, false);
	}

	/// Positive means white is favored. Negative means black is favored.
	int heuristic(Random rand)
	{
		int score = 0;
		for (int y = 0; y < 8; y++)
		{
			for (int x = 0; x < 8; x++)
			{
				int p = getPiece(x, y);
				int value;
				switch (p)
				{
					case None:
						value = 0;
						break;
					case Pawn:
						value = 10;
						break;
					case Rook:
						value = 63;
						break;
					case Knight:
						value = 31;
						break;
					case Bishop:
						value = 36;
						break;
					case Queen:
						value = 88;
						break;
					case King:
						value = 500;
						break;
					default:
						throw new RuntimeException("what?");
				}
				if (isWhite(x, y))
				{ score += value; }
				else
				{ score -= value; }
			}
		}
		return score + rand.nextInt(3) - 1;
	}

	/// Returns an iterator that iterates over all possible moves for the specified color
	ChessMoveIterator iterator(boolean white)
	{
		return new ChessMoveIterator(this, white);
	}

	/// Returns true iff the parameters represent a valid move
	boolean isValidMove(int xSrc, int ySrc, int xDest, int yDest)
	{
		ArrayList<Integer> possible_moves = moves(xSrc, ySrc);
		for (int i = 0; i < possible_moves.size(); i += 2)
		{
			if (possible_moves.get(i).intValue() == xDest && possible_moves.get(i + 1).intValue() == yDest)
			{ return true; }
		}
		return false;
	}

	/// Print a representation of the board to the specified stream
	void printBoard(PrintStream stream)
	{
		stream.println("  A  B  C  D  E  F  G  H");
		stream.print(" +");
		for (int i = 0; i < 8; i++)
		{ stream.print("--+"); }
		stream.println();
		for (int j = 7; j >= 0; j--)
		{
			stream.print(Character.toString((char) (49 + j)));
			stream.print("|");
			for (int i = 0; i < 8; i++)
			{
				int p = getPiece(i, j);
				if (p != None)
				{
					if (isWhite(i, j))
					{ stream.print("w"); }
					else
					{ stream.print("b"); }
				}
				switch (p)
				{
					case None:
						stream.print("  ");
						break;
					case Pawn:
						stream.print("p");
						break;
					case Rook:
						stream.print("r");
						break;
					case Knight:
						stream.print("n");
						break;
					case Bishop:
						stream.print("b");
						break;
					case Queen:
						stream.print("q");
						break;
					case King:
						stream.print("K");
						break;
					default:
						stream.print("?");
						break;
				}
				stream.print("|");
			}
			stream.print(Character.toString((char) (49 + j)));
			stream.print("\n +");
			for (int i = 0; i < 8; i++)
			{ stream.print("--+"); }
			stream.println();
		}
		stream.println("  A  B  C  D  E  F  G  H");
	}

	/// Pass in the coordinates of a square with a piece on it
	/// and it will return the places that piece can move to.
	ArrayList<Integer> moves(int col, int row)
	{
		ArrayList<Integer> pOutMoves = new ArrayList<Integer>();
		int p = getPiece(col, row);
		boolean bWhite = isWhite(col, row);
		int nMoves = 0;
		int i, j;
		switch (p)
		{
			case Pawn:
				if (bWhite)
				{
					if (! checkPawnMove(pOutMoves, col, inc(row), false, bWhite) && row == 1)
					{ checkPawnMove(pOutMoves, col, inc(inc(row)), false, bWhite); }
					checkPawnMove(pOutMoves, inc(col), inc(row), true, bWhite);
					checkPawnMove(pOutMoves, dec(col), inc(row), true, bWhite);
				}
				else
				{
					if (! checkPawnMove(pOutMoves, col, dec(row), false, bWhite) && row == 6)
					{ checkPawnMove(pOutMoves, col, dec(dec(row)), false, bWhite); }
					checkPawnMove(pOutMoves, inc(col), dec(row), true, bWhite);
					checkPawnMove(pOutMoves, dec(col), dec(row), true, bWhite);
				}
				break;
			case Bishop:
				for (i = inc(col), j = inc(row); true; i = inc(i), j = inc(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = dec(col), j = inc(row); true; i = dec(i), j = inc(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = inc(col), j = dec(row); true; i = inc(i), j = dec(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = dec(col), j = dec(row); true; i = dec(i), j = dec(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				break;
			case Knight:
				checkMove(pOutMoves, inc(inc(col)), inc(row), bWhite);
				checkMove(pOutMoves, inc(col), inc(inc(row)), bWhite);
				checkMove(pOutMoves, dec(col), inc(inc(row)), bWhite);
				checkMove(pOutMoves, dec(dec(col)), inc(row), bWhite);
				checkMove(pOutMoves, dec(dec(col)), dec(row), bWhite);
				checkMove(pOutMoves, dec(col), dec(dec(row)), bWhite);
				checkMove(pOutMoves, inc(col), dec(dec(row)), bWhite);
				checkMove(pOutMoves, inc(inc(col)), dec(row), bWhite);
				break;
			case Rook:
				for (i = inc(col); true; i = inc(i))
				{
					if (checkMove(pOutMoves, i, row, bWhite))
					{ break; }
				}
				for (i = dec(col); true; i = dec(i))
				{
					if (checkMove(pOutMoves, i, row, bWhite))
					{ break; }
				}
				for (j = inc(row); true; j = inc(j))
				{
					if (checkMove(pOutMoves, col, j, bWhite))
					{ break; }
				}
				for (j = dec(row); true; j = dec(j))
				{
					if (checkMove(pOutMoves, col, j, bWhite))
					{ break; }
				}
				break;
			case Queen:
				for (i = inc(col); true; i = inc(i))
				{
					if (checkMove(pOutMoves, i, row, bWhite))
					{ break; }
				}
				for (i = dec(col); true; i = dec(i))
				{
					if (checkMove(pOutMoves, i, row, bWhite))
					{ break; }
				}
				for (j = inc(row); true; j = inc(j))
				{
					if (checkMove(pOutMoves, col, j, bWhite))
					{ break; }
				}
				for (j = dec(row); true; j = dec(j))
				{
					if (checkMove(pOutMoves, col, j, bWhite))
					{ break; }
				}
				for (i = inc(col), j = inc(row); true; i = inc(i), j = inc(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = dec(col), j = inc(row); true; i = dec(i), j = inc(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = inc(col), j = dec(row); true; i = inc(i), j = dec(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				for (i = dec(col), j = dec(row); true; i = dec(i), j = dec(j))
				{
					if (checkMove(pOutMoves, i, j, bWhite))
					{ break; }
				}
				break;
			case King:
				checkMove(pOutMoves, inc(col), row, bWhite);
				checkMove(pOutMoves, inc(col), inc(row), bWhite);
				checkMove(pOutMoves, col, inc(row), bWhite);
				checkMove(pOutMoves, dec(col), inc(row), bWhite);
				checkMove(pOutMoves, dec(col), row, bWhite);
				checkMove(pOutMoves, dec(col), dec(row), bWhite);
				checkMove(pOutMoves, col, dec(row), bWhite);
				checkMove(pOutMoves, inc(col), dec(row), bWhite);
				break;
			default:
				break;
		}
		return pOutMoves;
	}

	/// Moves the piece from (xSrc, ySrc) to (xDest, yDest). If this move
	/// gets a pawn across the board, it becomes a queen. If this move
	/// takes a king, then it will remove all pieces of the same color as
	/// the king that was taken and return true to indicate that the move
	/// ended the game.
	boolean move(int xSrc, int ySrc, int xDest, int yDest)
	{
		if (xSrc < 0 || xSrc >= 8 || ySrc < 0 || ySrc >= 8)
		{ throw new RuntimeException("out of range"); }
		if (xDest < 0 || xDest >= 8 || yDest < 0 || yDest >= 8)
		{ throw new RuntimeException("out of range"); }
		int target = getPiece(xDest, yDest);
		int p = getPiece(xSrc, ySrc);
		if (p == None)
		{ throw new RuntimeException("There is no piece in the source location"); }
		if (target != None && isWhite(xSrc, ySrc) == isWhite(xDest, yDest))
		{ throw new RuntimeException("It is illegal to take your own piece"); }
		if (p == Pawn && (yDest == 0 || yDest == 7))
		{
			p = Queen; // a pawn that crosses the board becomes a queen
		}
		boolean white = isWhite(xSrc, ySrc);
		setPiece(xDest, yDest, p, white);
		setPiece(xSrc, ySrc, None, true);
		if (target == King)
		{
			// If you take the opponent's king, remove all of the opponent's pieces. This
			// makes sure that look-ahead strategies don't try to look beyond the end of
			// the game (example: sacrifice a king for a king and some other piece.)
			int x, y;
			for (y = 0; y < 8; y++)
			{
				for (x = 0; x < 8; x++)
				{
					if (getPiece(x, y) != None)
					{
						if (isWhite(x, y) != white)
						{
							setPiece(x, y, None, true);
						}
					}
				}
			}
			return true;
		}
		return false;
	}

	static int inc(int pos)
	{
		if (pos < 0 || pos >= 7)
		{ return - 1; }
		return pos + 1;
	}

	static int dec(int pos)
	{
		if (pos < 1)
		{ return - 1; }
		return pos - 1;
	}

	boolean checkMove(ArrayList<Integer> pOutMoves, int col, int row, boolean bWhite)
	{
		if (col < 0 || row < 0)
		{ return true; }
		int p = getPiece(col, row);
		if (p > 0 && isWhite(col, row) == bWhite)
		{ return true; }
		pOutMoves.add(col);
		pOutMoves.add(row);
		return (p > 0);
	}

	boolean checkPawnMove(ArrayList<Integer> pOutMoves, int col, int row, boolean bDiagonal, boolean bWhite)
	{
		if (col < 0 || row < 0)
		{ return true; }
		int p = getPiece(col, row);
		if (bDiagonal)
		{
			if (p == None || isWhite(col, row) == bWhite)
			{ return true; }
		}
		else
		{
			if (p > 0)
			{ return true; }
		}
		pOutMoves.add(col);
		pOutMoves.add(row);
		return (p > 0);
	}

	/// Represents a possible  move
	static class ChessMove
	{
		int xSource;
		int ySource;
		int xDest;
		int yDest;
	}

	/// Iterates through all the possible moves for the specified color.
	static class ChessMoveIterator
	{
		int x, y;
		ArrayList<Integer> moves;
		ChessState state;
		boolean white;

		/// Constructs a move iterator
		ChessMoveIterator(ChessState curState, boolean whiteMoves)
		{
			x = - 1;
			y = 0;
			moves = null;
			state = curState;
			white = whiteMoves;
			advance();
		}

		private void advance()
		{
			if (moves != null && moves.size() >= 2)
			{
				moves.remove(moves.size() - 1);
				moves.remove(moves.size() - 1);
			}
			while (y < 8 && (moves == null || moves.size() < 2))
			{
				if (++ x >= 8)
				{
					x = 0;
					y++;
				}
				if (y < 8)
				{
					if (state.getPiece(x, y) != ChessState.None && state.isWhite(x, y) == white)
					{ moves = state.moves(x, y); }
					else
					{ moves = null; }
				}
			}
		}

		/// Returns true iff there is another move to visit
		boolean hasNext()
		{
			return (moves != null && moves.size() >= 2);
		}

		/// Returns the next move
		ChessState.ChessMove next()
		{
			ChessState.ChessMove m = new ChessState.ChessMove();
			m.xSource = x;
			m.ySource = y;
			m.xDest = moves.get(moves.size() - 2);
			m.yDest = moves.get(moves.size() - 1);
			advance();
			return m;
		}
	}

	private boolean getTurn(boolean currentPlayer)
	{
		return ! currentPlayer;
	}

	int[] alphabeta(int depth, ChessState board, boolean isMaximizePlayer, int alpha, int beta)
	{
		int[] bestMoveForMax = new int[4];
		int[] bestMoveForMin = new int[4];
		ChessMoveIterator it;
		ChessState.ChessMove m;


		if (isMaximizePlayer)
		{
			it = board.iterator(true);
		}
		else
		{
			it = board.iterator(false);
		}
		// Check to see if its a tie, win, or lose
		// Win
		if (kingCaptured)
		{
			return new int[]{500000, 387565234, 235645, 45435344, 343535};

		}
/*		if (! it.hasNext())
		{
			return new int[]{board.heuristic(rand), 387565234, 235645, 45435344, 343535};
		}*/

		if (depth == 0 || ! it.hasNext())
		{
			//printBoard(board.cells);
			//System.out.println(depth);
			//numberOfMoves++;
			return new int[]{board.heuristic(rand), 387565234, 235645, 45435344, 343535};
		}

		ArrayList<int[]> scores = new ArrayList<>();
		while (it.hasNext())
		{
			// check to see if its the AI's turn
			ChessState newBoard = new ChessState(board);
			m = it.next();
			if (newBoard.isValidMove(m.xSource, m.ySource, m.xDest, m.yDest))
			{
				newBoard.kingCaptured = newBoard.move(m.xSource, m.ySource, m.xDest, m.yDest);
			}

			int[] score = alphabeta(depth - 1, newBoard, newBoard.getTurn(isMaximizePlayer), alpha, beta);
			if (isMaximizePlayer)
			{
				if (score[0] > alpha)
				{
					alpha = score[0];
					bestMoveForMax[0] = m.xSource;
					bestMoveForMax[1] = m.ySource;
					bestMoveForMax[2] = m.xDest;
					bestMoveForMax[3] = m.yDest;
				}
				if (alpha >= beta)
				{
					break;
				}
			}
			else
			{
				if (score[0] < beta)
				{
					beta = score[0];
					bestMoveForMin[0] = m.xSource;
					bestMoveForMin[1] = m.ySource;
					bestMoveForMin[2] = m.xDest;
					bestMoveForMin[3] = m.yDest;
				}
				if (alpha >= beta)
				{
					break;
				}
			}
		}

		if (isMaximizePlayer)
		{
			return new int[]{alpha, bestMoveForMax[0], bestMoveForMax[1], bestMoveForMax[2], bestMoveForMax[3]};
		}
		else
		{
			return new int[]{beta, bestMoveForMin[0], bestMoveForMin[1], bestMoveForMin[2], bestMoveForMin[3]};
		}
	}


	public static void main(String[] args) throws FileNotFoundException
	{
		int firstArg = 0;
		int secondArg = 0;

		ChessState chess = new ChessState();
		if (args.length > 0)
		{
			try
			{
				firstArg = Integer.parseInt(args[0]);
				secondArg = Integer.parseInt(args[1]);
			} catch (NumberFormatException e)
			{
				System.err.println("Argument" + args[0] + " must be an integer.");
				System.exit(1);
			}
		}
		if (firstArg == 0 && secondArg == 0)
		{
			chess.PlayerVsPlayer(firstArg, secondArg);
		}
		else if (firstArg == 0)
		{
			chess.PlayerVsAI(firstArg, secondArg);
		}
		else if (secondArg == 0)
		{
			chess.AIvsPlayer(firstArg, secondArg);
		}
		else if (firstArg > 0 || secondArg > 0)
		{
			chess.AIvsAI(firstArg, secondArg);
		}
	}

	private void run()
	{

	}

	private int[] parseInput(String userInput)
	{
		char[] chars = userInput.toCharArray();
		//H = 17

		int h = Character.getNumericValue('F');
		return new int[]{Character.getNumericValue(chars[0]) - 10, Character.getNumericValue(chars[1]) - 1, Character.getNumericValue(chars[2]) - 10, Character.getNumericValue(chars[3]) - 1};
	}

	private void PlayerVsPlayer(int firstArg, int secondArg) throws FileNotFoundException
	{
		String fileNamePlayerOne;
		String fileNamePlayerTwo;
		Scanner fileScPlayerOne = new Scanner(System.in);
		Scanner fileScPlayerTwo = new Scanner(System.in);
		Scanner fileOrInputSc = new Scanner(System.in);
		Scanner inputFromFileScPlayerOne = null;
		Scanner inputFromFileScPlayerTwo = null;
		Scanner consoleScPlayerOne = new Scanner(System.in);
		Scanner consoleScPlayerTwo = new Scanner(System.in);
		boolean fromFileOrFromConsole = false;


		boolean whiteHasWon = false;
		boolean darkHasWon = false;
		PrintStream print;
		String userOneInput;
		String userTwoInput;
		print = new PrintStream("Output.txt");


		ChessState board = new ChessState();
		board.resetBoard();
		System.out.println("Choose your Input:");
		System.out.println("1) From file ");
		System.out.println("2) From console ");
		System.out.print("Your Choice: ");
		int input = fileOrInputSc.nextInt();

		if (input == 1)
		{
			System.out.println("What file do you want to pipe from for Player One? ");
			System.out.print("Your File: ");
			fileNamePlayerOne = fileScPlayerOne.next();
			inputFromFileScPlayerOne = new Scanner(new File(fileNamePlayerOne));
			System.out.println();

			System.out.println("What file do you want to pipe from for Player One? ");
			System.out.print("Your File: ");
			fileNamePlayerTwo = fileScPlayerTwo.next();
			inputFromFileScPlayerTwo = new Scanner(new File(fileNamePlayerTwo));
		}

		while (true)
		{
			if (input == 1)
			{
				userOneInput = inputFromFileScPlayerOne.next();
			}
			else
			{
				board.printBoard(print);
				System.out.println("Example move:B3C3");
				System.out.print("Your Move: ");
				userOneInput = consoleScPlayerOne.next();
			}
			// if the user enters q quit the game
			if (userOneInput.equals("q"))
			{
				break;
			}

			int[] bestMoveForWhite = parseInput(userOneInput);
			//Validate move
			while (true)
			{
				if (board.isValidMove(bestMoveForWhite[0], bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3]))
				{
					if (board.move(bestMoveForWhite[0], bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3]))
					{
						whiteHasWon = true;
					}
					break;
				}
				else
				{
					System.out.print("Invalid move, Please Try again: ");
					userOneInput = consoleScPlayerOne.next();
				}
				// if the user enters q quit the game
				if (userOneInput.equals("q"))
				{
					break;
				}
				bestMoveForWhite = parseInput(userOneInput);
			}
			board.printBoard(System.out);
			System.out.println();

			if (whiteHasWon)
			{
				break;
			}


			if (input == 1)
			{

				userTwoInput = inputFromFileScPlayerTwo.next();
			}
			else
			{
				board.printBoard(print);
				System.out.println("Example move:B3C3");
				System.out.print("Your Move: ");
				userTwoInput = consoleScPlayerTwo.next();
			}
			// if the user enters q quit the game
			if (userTwoInput.equals("q"))
			{
				break;
			}

			int[] bestMoveForDark = parseInput(userTwoInput);
			//Validate move
			while (true)
			{
				if (board.isValidMove(bestMoveForDark[0], bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3]))
				{

					if (board.move(bestMoveForDark[0], bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3]))
					{
						darkHasWon = true;
					}
					break;
				}
				else
				{
					System.out.print("Invalid move, Please Try again: ");
					userTwoInput = consoleScPlayerTwo.next();
				}
				// if the user enters q quit the game
				if (userTwoInput.equals("q"))
				{
					break;
				}
				bestMoveForDark = parseInput(userTwoInput);
			}
			board.printBoard(System.out);
			System.out.println();

			if (darkHasWon)
			{
				break;
			}
		}
		inputFromFileScPlayerOne.close();
		inputFromFileScPlayerTwo.close();

		if (whiteHasWon)
		{
			System.out.println("White has won");
		}
		else
		{
			System.out.println("Dark has won");
		}
	}

	private void AIvsPlayer(int firstArg, int secondArg) throws FileNotFoundException
	{
		Scanner fileSc = new Scanner(System.in);
		Scanner fileOrInputSc = new Scanner(System.in);
		Scanner fileNameSc = null;
		Scanner consoleSc = new Scanner(System.in);
		boolean fromFileOrFromConsole = false;


		boolean whiteHasWon = false;
		boolean darkHasWon = false;
		boolean hasntWon = true;
		boolean isValidMove = true;
		PrintStream print;
		String userInput;
		print = new PrintStream("Output.txt");


		ChessState board = new ChessState();
		board.resetBoard();
		System.out.println("Choose your Input:");
		System.out.println("1) From file ");
		System.out.println("2) From console ");
		System.out.print("Your Choice: ");
		int input = fileOrInputSc.nextInt();
		if (input == 1)
		{
			System.out.println("What file do you want to pipe from? ");
			System.out.print("Your File: ");
			String fileName = fileSc.next();
			fileNameSc = new Scanner(new File(fileName));
		}
		while (true)
		{
			//AI
			int[] bestMoveForWhite = board.alphabeta(firstArg, board, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (board.isValidMove(bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3], bestMoveForWhite[4]))
			{
				if (board.move(bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3], bestMoveForWhite[4]))
				{
					whiteHasWon = true;
					break;
				}
			}
			board.printBoard(System.out);
			System.out.println();

			if (input == 1)
			{
				userInput = fileNameSc.next();
			}
			else
			{
				board.printBoard(print);
				System.out.println("Example move:B3C3");
				System.out.print("Your Move: ");
				userInput = consoleSc.next();
			}
			// if the user enters q quit the game
			if (userInput.equals("q"))
			{
				break;
			}

			int[] bestMoveForDark = parseInput(userInput);
			//Validate move
			while (true)
			{
				if (board.isValidMove(bestMoveForDark[0], bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3]))
				{
					if (board.move(bestMoveForDark[0], bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3]))
					{
						darkHasWon = true;
					}
					break;
				}
				else
				{
					System.out.print("Invalid move, Please Try again: ");
					userInput = consoleSc.next();
				}
				// if the user enters q quit the game
				if (userInput.equals("q"))
				{
					break;
				}
				bestMoveForDark = parseInput(userInput);
			}
			board.printBoard(System.out);
			System.out.println();

			if(darkHasWon){
				break;
			}
	}
		fileNameSc.close();
		if (whiteHasWon)
		{
			System.out.println("White has won");
		}
		else
		{
			System.out.println("Dark has won");
		}
	}

	private void PlayerVsAI(int firstArg, int secondArg) throws FileNotFoundException
	{
		int[] bestMoveForDark;
		Scanner fileSc = new Scanner(System.in);
		Scanner fileOrInputSc = new Scanner(System.in);
		Scanner fileNameSc = null;
		Scanner consoleSc = new Scanner(System.in);
		boolean fromFileOrFromConsole = false;


		boolean whiteHasWon = false;
		boolean darkHasWon = false;
		boolean hasntWon = true;
		boolean isValidMove = true;
		PrintStream print;
		String userInput;
		print = new PrintStream("Output.txt");


		ChessState board = new ChessState();
		board.resetBoard();
		System.out.println("Choose your Input:");
		System.out.println("1) From file ");
		System.out.println("2) From console ");
		System.out.print("Your Choice: ");
		int input = fileOrInputSc.nextInt();
		if (input == 1)
		{
			System.out.println("What file do you want to pipe from? ");
			System.out.print("Your File: ");
			String fileName = fileSc.next();
			fileNameSc = new Scanner(new File(fileName));
		}
		while (true)
		{
			if (input == 1)
			{
				userInput = fileNameSc.next();
			}
			else
			{
				board.printBoard(print);
				System.out.println("Example move:B3C3");
				System.out.print("Your Move: ");
				userInput = consoleSc.next();
			}
			// if the user enters q quit the game
			if (userInput.equals("q"))
			{
				break;
			}

			int[] bestMoveForWhite = parseInput(userInput);
			//Validate move
			while (true)
			{
				if (board.isValidMove(bestMoveForWhite[0], bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3]))
				{
					if (board.move(bestMoveForWhite[0], bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3]))
					{
						whiteHasWon = true;
					}
					break;
				}
				else
				{
					System.out.print("Invalid move, Please Try again: ");
					userInput = consoleSc.next();
					bestMoveForWhite = parseInput(userInput);
				}
			}
			board.printBoard(System.out);
			System.out.println();

			if (whiteHasWon)
			{
				break;
			}
			//AI
			bestMoveForDark = board.alphabeta(secondArg, board, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (board.isValidMove(bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3], bestMoveForDark[4]))
			{
				if (board.move(bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3], bestMoveForDark[4]))
				{

					break;
				}
			}

			board.printBoard(System.out);
			System.out.println();
		}
		fileNameSc.close();
		if (whiteHasWon)
		{
			System.out.println("White has won");
		}
		else
		{
			System.out.println("Dark has won");
		}
	}

	private void AIvsAI(int firstArg, int secondArg) throws FileNotFoundException
	{
		int[] bestMoveForWhite;
		int[] bestMoveForDark;
		boolean whiteHasWon = false;
		boolean darkHasWon = false;
		boolean hasntWon = true;
		PrintStream print;
		print = new PrintStream("OutputToFile");

		if (firstArg == 2 && secondArg == 4)
		{
			firstArg += 1;
		}
		ChessState s = new ChessState();
		s.resetBoard();
		s.printBoard(System.out);
		System.out.println();
		while (hasntWon)
		{
			bestMoveForWhite = s.alphabeta(firstArg, s, true, Integer.MIN_VALUE, Integer.MAX_VALUE);
			if (s.isValidMove(bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3], bestMoveForWhite[4]))
			{
				if (s.move(bestMoveForWhite[1], bestMoveForWhite[2], bestMoveForWhite[3], bestMoveForWhite[4]))
				{
					whiteHasWon = true;
					break;
				}
			}

			s.printBoard(System.out);
			System.out.println();
			if (! whiteHasWon)
			{
				bestMoveForDark = s.alphabeta(secondArg, s, false, Integer.MIN_VALUE, Integer.MAX_VALUE);
				if (s.isValidMove(bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3], bestMoveForDark[4]))
				{
					if (s.move(bestMoveForDark[1], bestMoveForDark[2], bestMoveForDark[3], bestMoveForDark[4]))
					{
						darkHasWon = true;
						hasntWon = false;
					}
				}
			}
			s.printBoard(System.out);
			System.out.println();
		}
		if (whiteHasWon)
		{
			System.out.println("White has won");
		}
		else
		{
			System.out.println("Black has won");
		}
	}
}



