package br.unifor.catalogo.persistence.manager.test;

import java.util.ArrayList;
import java.util.List;

public class TestManager {
	
	public List<Test> tests;
	public Integer id = 0;
	
	public TestManager(){
		tests = new ArrayList<Test>(); 
	}
	
	public Test newTest(){
		Test test = new Test(id++);
		tests.add(test);
		return test;
	}
	

	public class Test{
		public final Integer id;
		public Integer time = 0 ;
		public Integer operations = 0;
		
		private Test(Integer id){
			this.id = id;
		}
		
		public void sumOperation(Long time){
			this.time += time;
			operations++;
		}
		
	}
}
