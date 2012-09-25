package eu.monnetproject.rfcmapper;

import eu.monnetproject.lang.*;
import java.io.*;
import java.net.URL;
import java.util.*;
import javax.servlet.*;
import javax.servlet.http.*;

public class RFCMapperServlet extends HttpServlet {
	
	private static final HashMap<String,Set<String>> walsCodes = new HashMap<String,Set<String>>();
	private static final HashMap<String,Set<String>> glottologCodes = new HashMap<String,Set<String>>();
	private static final HashMap<String,Set<String>> idsidCodes = new HashMap<String,Set<String>>();
	
	@Override
	public void init(ServletConfig config) {
		final Scanner scan = new Scanner(config.getServletContext().getResourceAsStream("/languageids.csv"));
		while(scan.hasNextLine()) {
			final String line = scan.nextLine();
			final String[] tags = line.split(",");
			if(!walsCodes.containsKey(tags[0])) {
				walsCodes.put(tags[0], new HashSet<String>());
				glottologCodes.put(tags[0], new HashSet<String>());
				idsidCodes.put(tags[0], new HashSet<String>());
			}
			walsCodes.get(tags[0]).add(tags[1]);
			glottologCodes.get(tags[0]).add(tags[2]);
			idsidCodes.get(tags[0]).add(tags[3]);
		}
	}
	
	
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException, ServletException {
		if(req.getPathInfo() == null || !req.getPathInfo().startsWith("/")) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND);
			return;
		}
		final String id = req.getPathInfo().substring(1);
		final String prefix = getUrl(req);
		if(req.getPathInfo().startsWith("/prop/")) {
			handleProp(req,resp,prefix);
			return;
		}
		final Language lang;
		try {
			lang = Language.get(id);
		} catch(LanguageCodeFormatException x) {
			resp.sendError(HttpServletResponse.SC_NOT_FOUND,x.getMessage());
			return;
		}
		if(lang.getIso639_1() == null) {
			try {
				new URL("http://www.lexvo.org/page/iso639-3/" + lang.getIso639_3()).openStream();
				
			} catch(IOException x) {
				resp.sendError(HttpServletResponse.SC_NOT_FOUND,"Bad three letter ISO-639 code");
				return;
			}
		}
		resp.setContentType("application/rdf+xml");
		resp.setCharacterEncoding("UTF-8");
		final PrintWriter out = resp.getWriter();
		out.println("<?xml version=\"1.0\"?>");
		out.println("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+ 
			"xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" "+ 
			"xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "+ 
			"xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" "+ 
			"xmlns:ietflang=\""+prefix+"/prop/\" "+
			"xmlns:lexvo=\"http://lexvo.org/ontology#\" " +
			"xml:base=\""+prefix+"/\">");
		out.println("  <rdf:Description rdf:about=\""+prefix+"/"+lang.toString()+"\">");
		if(lang.getIso639_1() != null) {
			out.println("    <ietflang:iso639-1>" + lang.getIso639_1() + "</ietflang:iso639-1>");
		}
		if(lang.getIso639_2() != null) {
			out.println("    <ietflang:iso639-2>" + lang.getIso639_2() + "</ietflang:iso639-2>");
		}
		if(lang.getIso639_3() != null) {
			out.println("    <ietflang:iso639-3>" + lang.getIso639_3() + "</ietflang:iso639-3>");
		}
		if(lang.getScript() != null) {
			out.println("    <ietflang:iso15924-alpha4>" + lang.getScript().getAlpha4code() + "</ietflang:iso15924-alpha4>");
			out.println("    <ietflang:iso15924-numeric>" + lang.getScript().getNumericCode() + "</ietflang:iso15924-numeric>");
		}
		if(lang.getRegion() != null) {
			out.println("    <ietflang:iso3166-alpha2>" + lang.getRegion().getAlpha2code() + "</ietflang:iso3166-alpha2>");
			out.println("    <ietflang:iso3166-alpha3>" + lang.getRegion().getAlpha3code() + "</ietflang:iso3166-alpha3>");
			out.println("    <ietflang:iso3166-numeric>" + lang.getRegion().getNumericCode() + "</ietflang:iso3166-numeric>");
			out.println("    <lexvo:usedIn rdf:resource=\"http://ontologi.es/place/" + lang.getRegion() + "\"/>");
			out.println("    <lexvo:usedIn rdf:resource=\"http://www.lexvo.org/page/iso3166/" + lang.getRegion() + "\"/>");
		}
		for(String variant : lang.getVariants()) {
			out.println("    <ietflang:variant>"+variant+"</ietflang:variant>");
		}
		for(String extension : lang.getExtensions()) {
			out.println("    <ietflang:extension>"+extension+"</ietflang:extension>");
		}
		if(lang.getPrivateUse() != null) {
			out.println("    <ietflang:private-use>"+lang.getPrivateUse()+"</ietflang:private-use>");
		}
		if(lang.getScript() == null && lang.getRegion() == null && lang.getVariants().isEmpty() && lang.getExtensions().isEmpty() && lang.getPrivateUse() == null) {
			out.println("    <owl:sameAs rdf:resource=\"http://www.lexvo.org/page/iso639-3/" + lang.getIso639_3() + "\"/>");
			if(lang.getName() != null) {
				out.println("    <rdfs:label xml:lang=\"eng\">" + lang.getName() + "</rdfs:label>");
			}
			if(lang.getNativeName() != null) {
				out.println("    <rdfs:label xml:lang=\""+lang.getIso639_3()+"\">" + lang.getNativeName() + "</rdfs:label>");
			}
			
		} else {
			out.println("    <skos:broader rdf:resource=\"http://www.lexvo.org/page/iso639-3/" + lang.getIso639_3() + "\"/>");
		}
		if(walsCodes.containsKey(lang.getIso639_3())) {
			for(String walsCode : walsCodes.get(lang.getIso639_3())) {
				out.println("    <rdfs:seeAlso rdf:resource=\"http://wals.info/language/" + walsCode + "\"/>");
			}
		}
		if(glottologCodes.containsKey(lang.getIso639_3())) {
			for(String glottologCode : glottologCodes.get(lang.getIso639_3())) {
				out.println("    <rdfs:seeAlso rdf:resource=\"http://www.glottolog.org/resource/languoid/id/" + glottologCode + "\"/>");
			}
		}
		if(idsidCodes.containsKey(lang.getIso639_3())) {
			for(String idsidCode : idsidCodes.get(lang.getIso639_3())) {
				out.println("    <rdfs:seeAlso rdf:resource=\"http://http://lingweb.eva.mpg.de/ids/language/" + idsidCode + "\"/>");
			}
		}
		out.println("  </rdf:Description>");
		out.println("</rdf:RDF>");
		
	}
	
	private static final String[] props = new String[] {
		"iso639-1",
		"iso639-2",
		"iso639-3",
		"iso15924-alpha4",
		"iso15924-numeric",
		"iso3166-alpha2",
		"iso3166-alpha3",
		"iso3166-numeric",
		"variant",
		"extension",
		"private-use" 
	};
	
	
	private static void handleProp(HttpServletRequest req, HttpServletResponse resp, String prefix) throws IOException {
		final String id = req.getPathInfo().substring(6);
		for(String prop : props) {
			if(id.equals(prop)) {
				resp.setContentType("application/rdf+xml");
				resp.setCharacterEncoding("UTF-8");
				final PrintWriter out = resp.getWriter();
				out.println("<?xml version=\"1.0\"?>");
				out.println("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" "+ 
					"xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\" "+ 
					"xmlns:owl=\"http://www.w3.org/2002/07/owl#\" "+ 
					"xmlns:skos=\"http://www.w3.org/2004/02/skos/core#\" "+ 
					"xmlns:rfc4646=\""+prefix+"/prop/\" "+
					"xml:base=\""+prefix+"/\">");
				out.println("  <rdf:Property rdf:about=\""+prefix+"/prop/" + id + "\"/>");
				out.println("</rdf:RDF>");
				out.close();
				return;
			}
		}
		resp.sendError(HttpServletResponse.SC_NOT_FOUND);
	}
	
	
	public static String getUrl(HttpServletRequest req) {
		String scheme = req.getScheme();             // http
		String serverName = req.getServerName();     // hostname.com
		int serverPort = req.getServerPort();        // 80
		String contextPath = req.getContextPath();   // /mywebapp
		String servletPath = req.getServletPath();   // /servlet/MyServlet
		
		// Reconstruct original requesting URL
		String url = scheme+"://"+serverName+(serverPort == 80 ? "" : ":"+serverPort)+contextPath+servletPath;
		return url;
	}
}