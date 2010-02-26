package br.unifor.onaga.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.onaga.occi.xml.Compute;
import br.unifor.onaga.occi.xml.Computes;
import br.unifor.onaga.web.OCCIClient;

/**
 * Servlet implementation class OCCIComputeServlet
 */
public class OCCIComputeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OCCIComputeServlet() {
        super();
        // TODO Auto-generated constructor stub
    }
    
    private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	String acao = request.getParameter("acao");    	
    	if("create".equals(acao)){
    		create(request, response);
    	}else if("show".equals(acao)){
    		show(request, response);
    	}else if("list".equals(acao)){
    		list(request, response);
    	}else if("delete".equals(acao)){
    		delete(request, response);
    	}
    	
    }
    
    private void delete(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id = (String)request.getParameter("id");
		OCCIClient.deploy_vm(id);
		
		request.getRequestDispatcher("/jsp/OCCI/computeList.jsp").forward(
				request, response);
	}

	private void create(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id = (String)request.getParameter("id");
		OCCIClient.deploy_vm(id);
		
		request.getRequestDispatcher("/jsp/OCCI/computeList.jsp").forward(
				request, response);
	}
	
	private void show(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Compute retorno = OCCIClient.show_compute(request.getParameter("id"));
		request.setAttribute("item", retorno);
		request.getRequestDispatcher("/jsp/OCCI/computeShow.jsp").forward(
				request, response);
	}
	
	private void list(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Computes retorno = OCCIClient.list_compute();
		request.setAttribute("items", retorno);
		request.getRequestDispatcher("/jsp/OCCI/computeList.jsp").forward(
				request, response);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

}
