package edu.upenn.cis455.hw1;

import junit.framework.TestCase;

public class HelloWorldTest extends TestCase {
//  public void testA() {
//	  System.out.println(String.format("<button onclick=\"window.location.href = 'http://localhost.com:%d/shutdown';\">Shutdown</button>", 8080));
//	  assertTrue(true);
//  }
  
  public void testEnqueBlock() {
	  BlockingQueue<Integer> q = new BlockingQueue<Integer>(10);
	  
	  Thread enqueuer = new Thread (() -> {
		  try {
			  for(int i = 0; i < 11; i++) {
					q.enqueue(Integer.valueOf(i));			  
			  }
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  });
	  enqueuer.start();
	  try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  assertEquals(Thread.State.WAITING, enqueuer.getState());
	  

	  Thread dequeuer = new Thread (() -> {
		  try {
			  q.dequeue(0);	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  });
	  
	  dequeuer.start();
	  try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  assertEquals(dequeuer.getState(), Thread.State.TERMINATED);
	  assertEquals(enqueuer.getState(), Thread.State.TERMINATED);
	  enqueuer.interrupt();
	  dequeuer.interrupt();
	  
  }
  

  public void testDequeBlock() {
	  

	  BlockingQueue<Integer> q = new BlockingQueue<Integer>(10);

	  
	  Thread enqueuer = new Thread (() -> {
		  try {
			  for(int i = 0; i < 11; i++) {
					q.enqueue(Integer.valueOf(i));			  
			  }
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  });
	  
	  
	  Thread dequeuer = new Thread (() -> {
		  try {
			  q.dequeue(0);	
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	  });
	  dequeuer.start();
	  
	  
	  try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  
	  assertEquals(Thread.State.WAITING, dequeuer.getState());
	  
	  enqueuer.start();
	  try {
		Thread.sleep(1000);
	} catch (InterruptedException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	  assertEquals(dequeuer.getState(), Thread.State.TERMINATED);
	  assertEquals(enqueuer.getState(), Thread.State.TERMINATED);
	  enqueuer.interrupt();
	  dequeuer.interrupt();
	  
	  
  }
}
