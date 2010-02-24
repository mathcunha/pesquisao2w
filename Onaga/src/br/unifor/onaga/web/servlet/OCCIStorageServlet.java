package br.unifor.onaga.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.onaga.web.OCCIClient;

/**
 * Servlet implementation class OCCIStorageServlet
 */
public class OCCIStorageServlet extends HttpServlet {
	protected Logger log = Logger.getLogger(OCCIStorageServlet.class.getName()); 
	
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public OCCIStorageServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String retorno = OCCIClient.list_storage();
		
		response.setContentType("text/html");
	    PrintWriter out = response.getWriter();
	    out.print(log.getLevel());
	    out.print(retorno);

		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
