package br.unifor.onaga.web.servlet;

import java.io.File;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    
    private void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException {
    	String id = (String)request.getParameter("id");
    	OCCIClient.deploy_vm(id);
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
