package br.unifor.onaga.web.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.onaga.occi.xml.Disk;
import br.unifor.onaga.occi.xml.Storage;
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
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	private void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		String acao = request.getParameter("acao");

		if ("list".equals(acao)) {
			list(request, response);
		} else if ("show".equals(acao)) {
			show(request, response);
		}

	}

	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Storage retorno = OCCIClient.list_storage();
		request.setAttribute("storage", retorno);
		request.getRequestDispatcher("/jsp/OCCI/storageList.jsp").forward(
				request, response);
	}

	private void show(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Disk retorno = OCCIClient.show_storage(request.getParameter("id"));
		request.setAttribute("disk", retorno);
		request.getRequestDispatcher("/jsp/OCCI/storageShow.jsp").forward(
				request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

}
