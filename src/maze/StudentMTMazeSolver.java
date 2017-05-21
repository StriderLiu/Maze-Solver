package maze;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * This file needs to hold your solver to be tested. 
 * You can alter the class to extend any class that extends MazeSolver.
 * It must have a constructor that takes in a Maze.
 * It must have a solve() method that returns the datatype List<Direction>
 *   which will either be a reference to a list of steps to take or will
 *   be null if the maze cannot be solved.
 */

public class StudentMTMazeSolver extends SkippingMazeSolver {
	public ExecutorService pool;

	public StudentMTMazeSolver(Maze maze) {
		super(maze);
	}

	/*
	 * I use thread pool to deal with multithreading tasks
	 * Along each direction starting at the entry point, go all the way down to the first point that has multiple choices (> 1)
	 * Then respectively assign a task according to those points
	 * In each thread, use those points as the head of paths, and run iterative DFS from the head 
	 * @see maze.MazeSolver#solve()
	 */
	public List<Direction> solve() {
		// TODO: Implement your code here
		List<Direction> res = null;
		List<Future<List<Direction>>> futures = new LinkedList<Future<List<Direction>>>();
		LinkedList<DFS> tasks = new LinkedList<DFS>();
		int processors = Runtime.getRuntime().availableProcessors();
		pool = Executors.newFixedThreadPool(processors);
		
		try{
			Choice first = firstChoice(maze.getStart());
			
			int len = first.choices.size();
			for(int i = 0; i < len; i++) {
				Choice curChoice = follow(first.at, first.choices.peek());	
				tasks.add(new DFS(curChoice, first.choices.pop()));	
			}
		}catch (SolutionFound e) {
			System.out.println("Solution Found!");
		}
		
		try {
			futures = pool.invokeAll(tasks);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		pool.shutdown();
		
		for(Future<List<Direction>> ans : futures) {
			try {
				if(ans.get() != null)
					res = ans.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return res;
	}

	private class DFS implements Callable<List<Direction>> {
		Choice start;
		Direction direction;
		
		public DFS(Choice start, Direction direction) {
			this.start = start;
			this.direction = direction;
		}

		@Override
		public List<Direction> call() {
			// TODO Auto-generated method stub
			LinkedList<Choice> stack = new LinkedList<Choice>();
			Choice currChoice;

			try{
				stack.push(this.start);
				
				while(!stack.isEmpty()) {
					currChoice = stack.peek();

					if(currChoice.isDeadend()) {
						//backtracking
						stack.pop();
						if (!stack.isEmpty())
							stack.peek().choices.pop();
						continue;
					}
					stack.push(follow(currChoice.at, currChoice.choices.peek()));
				}
				return null;
			} catch (SolutionFound e) {
				Iterator<Choice> iter = stack.iterator();
	            LinkedList<Direction> solutionPath = new LinkedList<Direction>();
	        
	            while (iter.hasNext()) {
	            	currChoice = iter.next();
	                solutionPath.push(currChoice.choices.peek());
	            }
	            solutionPath.push(direction);
	            if (maze.display != null)
	            	maze.display.updateDisplay();
	            
	            return pathToFullPath(solutionPath);
			}
		}
	}
}