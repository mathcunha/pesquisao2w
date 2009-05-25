package br.unifor.catalogo.web.servlet;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.catalogo.persistence.FilmeTO;
import br.unifor.catalogo.persistence.manager.PersistenceDelegate;

/**
 * Servlet implementation class FilmeServlet
 */
public class FilmeServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public FilmeServlet() {
        super();        
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
		FilmeTO filme = new FilmeTO();		
		String strIdentificador = request.getParameter("identificador");
		String strNome = request.getParameter("nome");
		String strQuantidade = request.getParameter("quantidade");
		String strAcao = request.getParameter("acao");
		
		filme.setNome(strNome);
		filme.setQuantidade(new Long(strQuantidade));
		
		if(strIdentificador != null && strIdentificador.length() > 0){
			filme.setIdentificador(new Long(strIdentificador));
		}
		
		PersistenceDelegate delegate = PersistenceDelegate.getInstance();
		
		if(strAcao == null){
			if(filme.getIdentificador() == null){				
				delegate.insert(filme);
			}else{
				delegate.update(filme);
			}
		}
	}

}
