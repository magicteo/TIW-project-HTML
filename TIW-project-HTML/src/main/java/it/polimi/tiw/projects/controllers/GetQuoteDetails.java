package it.polimi.tiw.projects.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.projects.beans.Option;
import it.polimi.tiw.projects.beans.Product;
import it.polimi.tiw.projects.beans.Quote;
import it.polimi.tiw.projects.beans.User;
import it.polimi.tiw.projects.dao.OptionDAO;
import it.polimi.tiw.projects.dao.ProductDAO;
import it.polimi.tiw.projects.dao.QuoteDAO;
import it.polimi.tiw.projects.utils.ConnectionHandler;

@WebServlet("/GetQuoteDetails")
public class GetQuoteDetails extends HttpServlet{
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;

	
	public GetQuoteDetails() {
		super();
	}
	
	public void init() throws ServletException {
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
		connection = ConnectionHandler.getConnection(getServletContext());
	}
	
	
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// If the user is not logged in (not present in session) redirect to the login
		String loginpath = getServletContext().getContextPath() + "/loginPage.html";
		HttpSession session = request.getSession();
		if (session.isNew() || session.getAttribute("user") == null) {
			response.sendRedirect(loginpath);
			return;}
		
		
		User user = (User) session.getAttribute("user");
		
		// get and check params
				Integer quoteID = null;
				try {
					quoteID = Integer.parseInt(request.getParameter("quoteID"));
				} catch (NumberFormatException | NullPointerException e) {
					// only for debugging e.printStackTrace();
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Incorrect param values");
					return;
				}
		
		QuoteDAO quoteDao = new QuoteDAO(connection);
		Quote quote = new Quote();
		
		
		try {
			quote = quoteDao.findQuoteDetails(quoteID);
			
		} catch (SQLException e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover quote details");
			return;
		}
		
		try {
			if(quote==null) {
				throw new Exception ("Not possible to recover quote details");
			}
		}catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover quote details");
			return;
		}
		
		// check if the user is a Client and quote.clientUsername is equal to active Client username
		try {
			if (user.getEmployee()==false && !(user.getUsername().equals(quote.getClientUsername())))  {
				throw new Exception ("Not possible to recover quote details");
			}
		}catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "You can't access this element");
			return;
		}
		
		// check if the user is an Employee and quote.employeeUsername is equal to active Employee username
		try {
			if (user.getEmployee()==true && !(user.getUsername().equals(quote.getEmployeeUsername())))  {
				throw new Exception ("Not possible to recover quote details");
			}
		}catch (Exception e) {
			e.printStackTrace();
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "You can't access this element");
			return;
		}
		
				
		List<Option> selectedOptions = new ArrayList<Option>();	
		
		try {
			selectedOptions= quoteDao.findQuoteOptions(quoteID);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover selected options");
			return;
		}
				
		
		OptionDAO optionDao = new OptionDAO(connection);
		List<Option> sOpt = new ArrayList<Option>();
		
		for(Option opt: selectedOptions) {
			int temp = opt.getOptionID();
			Option o1 = new Option();
			try {
				o1 = optionDao.findOptionDetails(temp);
				sOpt.add(o1);
			}catch (SQLException e) {
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover option details");
				return;}
			}
		
		
		
		ProductDAO productDao = new ProductDAO(connection);
		Product product= new Product();
		
		try {
			product = productDao.findProductDetails(quote.getProductID());
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Not possible to recover product details");
			return;
		}
		
		
				
				
		// Redirect to the Quote Details page and add quote to the parameters
		String path = "/WEB-INF/QuoteDetails.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("quote", quote);
		ctx.setVariable("selectedOptions", sOpt);
		ctx.setVariable("product", product);
		templateEngine.process(path, ctx, response.getWriter());
	}
	
	

	
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

	public void destroy() {
		try {
			ConnectionHandler.closeConnection(connection);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

}
