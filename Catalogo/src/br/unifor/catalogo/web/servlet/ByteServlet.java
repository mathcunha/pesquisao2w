package br.unifor.catalogo.web.servlet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.catalogo.persistence.CatalogoTO;
import br.unifor.catalogo.persistence.FilmeTO;
import br.unifor.catalogo.persistence.manager.PersistenceDelegate;

/**
 * Servlet implementation class ByteServlet
 */
public class ByteServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ByteServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		execute(request, response);
	}

	protected void execute(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		FilmeTO filme = new FilmeTO();
		String strIdentificador = request.getParameter("identificador");
		String strNome = request.getParameter("nome");
		String strQuantidade = request.getParameter("quantidade");
		String strAcao = request.getParameter("acao");

		filme.setNome(strNome);

		if (strQuantidade != null && strQuantidade.length() > 0) {
			filme.setQuantidade(new Long(strQuantidade));
		}

		if (strIdentificador != null && strIdentificador.length() > 0) {
			filme.setIdentificador(new Long(strIdentificador));
		}

		PersistenceDelegate delegate = PersistenceDelegate.getInstance();

		if (strAcao == null) {
			if (filme.getIdentificador() == null) {
				delegate.insert(filme);
			} else {
				delegate.update(filme);
			}
			listar(request, response, filme, delegate);
		} else if ("excluir".equals(strAcao)) {
			delegate.delete(filme);
			listar(request, response, filme, delegate);
		} else if ("exibir".equals(strAcao)) {			
			filme.setIdentificador(delegate.findByPk(filme).getIdentificador());
			request.setAttribute("bean", filme);
			request.getRequestDispatcher("byte/editar.jsp").forward(request,
					response);
		} else if ("popular".equals(strAcao)) {
			popular();
			listar(request, response, filme, delegate);
		} else if ("listar".equals(strAcao)) {
			listar(request, response, filme, delegate);
		}

	}

	private void popular() throws SecurityException, IOException {
		PersistenceDelegate delegate = PersistenceDelegate.getInstance();
		FilmeTO filme = new FilmeTO(1l, "X-Men", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(2l, "X-Men 3", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(3l, "Simonal – Ninguém sabe o duro que dei  ",
				30l);
		delegate.insert(filme);

		filme = new FilmeTO(4l, "Palavra (en)cantada", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(5l, "Uma Noite no Museu 2", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(6l, "A Montanha Enfeitiçada", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(7l, "X-Men Origens: Wolverine", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(8l, "Anjos e Demônios", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(9l, "Monstros vs Alienígenas", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(10l, "A Festa do Garfield", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(11l, "Star Trek", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(12l, "Divã", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(13l, "Noivas Em Guerra", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(14l, "Lutador, O", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(15l, "Sete Vidas", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(16l, "Foi Apenas Um Sonho", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(17l, "Se Eu Fosse Você", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(18l, "Dúvida", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(19l, "Quarentena", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(20l, "Budapeste", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(21l, "Velozes e Furiosos", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(22l, "Desejo e Perigo", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(23l, "O senhor do aneis", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(24l, "loucademia de polícia", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(25l, "Shrek", 30l);
		delegate.insert(filme);

		filme = new FilmeTO(26l, "Matrix", 30l);
		delegate.insert(filme);
	}

	private void listar(HttpServletRequest request,
			HttpServletResponse response, FilmeTO filme,
			PersistenceDelegate delegate) throws ServletException, IOException {
		Map<String, CatalogoTO> mapa = delegate.findAll(filme);

		if (mapa != null) {
			Collection<CatalogoTO> colecao = mapa.values();

			List<CatalogoTO> lista = new ArrayList<CatalogoTO>(
					colecao.size() + 1);
			lista.addAll(colecao);

			Collections.sort(lista);

			request.setAttribute("itens", lista);
		}

		request.getRequestDispatcher("byte/listar.jsp").forward(request,
				response);
	}

}
