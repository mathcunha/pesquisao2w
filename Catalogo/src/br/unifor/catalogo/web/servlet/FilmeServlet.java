package br.unifor.catalogo.web.servlet;

import java.io.IOException;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.catalogo.persistence.CatalogoTO;
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
		
		if(strQuantidade != null && strQuantidade.length() > 0){
			filme.setQuantidade(new Long(strQuantidade));
		}
		
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
		}else if("excluir".equals(strAcao)){
			delegate.delete(filme);
		}else if("exibir".equals(strAcao)){
			filme = (FilmeTO) delegate.findByPk(filme);
			request.setAttribute("bean", filme);
			request.getRequestDispatcher("filme/editar.jsp").forward(request, response);
		}
		listar(request, response, filme, delegate);			
		
	}	

	private void listar(HttpServletRequest request, HttpServletResponse response, FilmeTO filme,
			PersistenceDelegate delegate) throws ServletException, IOException {
		Map<String, CatalogoTO> mapa = delegate.findAll(filme);
		
		request.setAttribute("itens", mapa.values());
		
		request.getRequestDispatcher("filme/listar.jsp").forward(request, response);
	}

}
