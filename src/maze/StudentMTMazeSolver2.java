package maze;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class StudentMTMazeSolver2 extends SkippingMazeSolver {
	public ExecutorService pool;

	public StudentMTMazeSolver2(Maze maze) {
		super(maze);
	}
	
	/*
	 * I use thread pool to deal with multi-threading tasks
	 * Along each direction starting at the entry point, go all the way down to the first point that has multiple choices (> 1)
	 * Then respectively assign a task according to those points
	 * In each thread, use those points as the head of paths, and run BFS from the head 
	 * @see maze.MazeSolver#solve()
	 */
	public List<Direction> solve() {
		// TODO Auto-generated method stub
		List<Direction> res = null;
		List<Future<List<Direction>>> futures = new LinkedList<Future<List<Direction>>>();
		LinkedList<BFS> tasks = new LinkedList<BFS>();
		int processors = Runtime.getRuntime().availableProcessors();
		pool = Executors.newFixedThreadPool(processors);
		
		try{
			Choice first = firstChoice(maze.getStart());
			
			int len = first.choices.size();
			for(int i = 0; i < len; i++) {
				Choice curChoice = follow(first.at, first.choices.peek());		
				tasks.add(new BFS(curChoice, first.choices.pop()));
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

	private class BFS implements Callable<List<Direction>> {
		Choice start;
		
		public BFS(Choice start, Direction choiceDir) {
			this.start = start;	
		}
		
		public List<Direction> call() {
	        SolutionNode curr = null;
	        LinkedList<SolutionNode> frontier = new LinkedList<SolutionNode>();

	        try {
	        	frontier.push(new SolutionNode(null, this.start));
	            while (!frontier.isEmpty()) {
	                LinkedList<SolutionNode> new_frontier = new LinkedList<SolutionNode>();
	                for (SolutionNode node : frontier) {
	                    if (!node.choice.isDeadend()) {
	                        curr = node;
	                        new_frontier.addAll(expand(node));
	                    }
	                    else if (maze.display != null) {
	                        maze.setColor(node.choice.at, 0);
	                    }
	                }
	                frontier = new_frontier;
	                if (maze.display != null) {
	                    maze.display.updateDisplay();
	                    try {
	                        Thread.sleep(50);
	                    }
	                    catch (InterruptedException e) {}
	                    // Could use: maze.display.waitForMouse();
	                    // if we wanted to pause until a mouse button was pressed.
	                }
	            }
	         
	            return null;
	        }
	        catch (SolutionFound e) {
	            if (curr == null) {
	                // this only happens if there was a direct path from the start
	                // to the end
	                return pathToFullPath(maze.getMoves(maze.getStart()));
	            }
	            else {
	                LinkedList<Direction> soln = new LinkedList<Direction>();
	                // First save the direction we were going in when the exit was
	                // discovered.
	                soln.addFirst(exploring);
	                
	                while (curr != null) {
	                    try {
	                        Choice walkBack = followMark(curr.choice.at, curr.choice.from, 1);
	                        if (maze.display != null)
	                            maze.display.updateDisplay();
	                        
	                        soln.addFirst(walkBack.from);
	                        curr = curr.parent;
	                    }
	                    catch (SolutionFound e2) {
	                        // If there is a choice point at the maze entrance, then
	                        // record
	                        // which direction we should choose.
	                        if (maze.getMoves(maze.getStart()).size() > 1) 
	                        	soln.addFirst(e2.from);
	                        if (maze.display != null) {
	                            markPath(soln, 1);
	                            maze.display.updateDisplay();
	                        }
	                        return pathToFullPath(soln);
	                    }
	                }
	                markPath(soln, 1);
	                return pathToFullPath(soln);
	            }
	        }
	    }
	}
	
	public class SolutionNode {
        public SolutionNode parent;
        public Choice choice;

        public SolutionNode(SolutionNode parent, Choice choice) {
            this.parent = parent;
            this.choice = choice;
        }
    }
	
	Direction exploring = null;
	/**
     * Expands a node in the search tree, returning the list of child nodes.
     * 
     * @throws SolutionFound
     */
    public List<SolutionNode> expand(SolutionNode node) throws SolutionFound {
        LinkedList<SolutionNode> res = new LinkedList<SolutionNode>();
        
        if (maze.display != null) 
        	maze.setColor(node.choice.at, 0);
        
        for (Direction dir : node.choice.choices) {
            exploring = dir;
            Choice newChoice = follow(node.choice.at, dir);
            if (maze.display != null) maze.setColor(newChoice.at, 2);
            res.add(new SolutionNode(node, newChoice));
        }
        return res;
    }
}
