package br.unifor.onaga.web.servlet.virtualappliance;

import java.io.IOException;

import javax.ejb.EJB;
import javax.imageio.spi.RegisterableService;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import br.unifor.onaga.ejb.entity.VirtualAppliance;
import br.unifor.onaga.ejb.session.RegisterSessionLocal;
import br.unifor.onaga.ejb.session.RegisterSessionRemote;
import br.unifor.onaga.occi.xml.Disk;
import br.unifor.onaga.occi.xml.Storage;
import br.unifor.onaga.web.OCCIClient;

/**
 * Servlet implementation class VirtualApplianceServlet
 */
public class VirtualApplianceServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	protected RegisterSessionRemote registerSession;
	
	@EJB(name = "RegisterSession")
	public void setCalculator(RegisterSessionRemote session)
	{
		registerSession = session;
	}

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public VirtualApplianceServlet() {
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

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request,
			HttpServletResponse response) throws ServletException, IOException {
		doRequest(request, response);
	}

	private void doRequest(HttpServletRequest request,
			HttpServletResponse response) throws IOException, ServletException {
		String acao = request.getParameter("acao");
		

		if ("list".equals(acao)) {
			list(request, response);
		}

	}

	private void list(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		
		
		request.setAttribute("list", registerSession.findAll(new VirtualAppliance()));
		request.getRequestDispatcher("/jsp/VirtualAppliance/list.jsp").forward(
				request, response);
	}

}
