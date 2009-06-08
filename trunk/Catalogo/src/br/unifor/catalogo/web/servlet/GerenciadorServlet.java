package br.unifor.catalogo.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.catalogo.persistence.manager.PersistenceDelegate;
import br.unifor.catalogo.persistence.manager.util.StateVerifier;

/**
 * Servlet implementation class GerenciadorServlet
 */
public class GerenciadorServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public GerenciadorServlet() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}
	
	protected void execute(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if("zerar".equals(request.getParameter("acao"))){
			zerar(request, response);
		}else if("resultado".equals(request.getParameter("acao"))){
			resultado(request, response);
		}
		else{
			response.getWriter().write(StateVerifier.calcularHash(PersistenceDelegate.getInstance().getRoot()));
		}
		
	}
	
	protected void resultado(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PersistenceDelegate.getInstance().printResult(response.getWriter());
	}
	
	protected void zerar(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		PersistenceDelegate.getInstance().newTest();
	}

}
