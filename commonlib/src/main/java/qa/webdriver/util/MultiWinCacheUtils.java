package qa.webdriver.util;

import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.util.StatusPrinter;

/**
 *  
 * @author Jon Austen
 *
 */
public abstract class MultiWinCacheUtils {

	public static WebDriver driver;
	public static SiteServer fs;
	public static final Logger logger = LoggerFactory.getLogger( "MultiWinCache" );
	public static String mainHandle = "";
	public static String mainWindowTitle = "";
	public static Set<String> handleCache = new HashSet<String>();	
	protected static PrintStream log = System.out; // deprecated by use of LogBack 

	public static void clearAndSetValue(WebElement field, String text) { 
		field.clear(); 
		field.sendKeys(Keys.chord(Keys.CONTROL, "a"), text); 
	}

	public static void closeAllBrowserWindows() {
		Set<String> handles = driver.getWindowHandles();
		if ( !handles.isEmpty() ) {
			logger.info("Closing " + handles.size() + " window(s).");
			for ( String windowId : handles ) {
				logger.info("-- Closing window handle: " + windowId );
				driver.switchTo().window( windowId ).close();
			}
		} else {
			logger.info("There were no window handles to close.");
		}
		driver.quit();  // this quit is critical, otherwise window will hang open
	}
	
	public static void closeWindowByHandle( String windowHandle ) {  
		driver.switchTo().window( windowHandle );
		logger.info("Closing window with title \"" + driver.getTitle() + "\"." );
		driver.close();
	}
	
	public static void returnLoggerState() {
		// print internal state
	    LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();
	    StatusPrinter.print(lc);
	}

	/**
	 * Loops to determine if WebDriver.getWindowHandles() returns any
	 *  additional windows that the allHandles cache does not currently
	 *  contain. If new windows are found, switch to latest window and
	 *  update allHandles cache.
	 */
	public static String handleNewWindow() {
		String newHandle = "";
		printHandles();
		Set<String> updatedHandles = driver.getWindowHandles();
		if ( updatedHandles.size() <= handleCache.size() ) {
			mainHandle = "";
			throw new IllegalStateException("This method handleNewWindow is not appropriate\n" +
					"in this case.  You are probably looking for the\n"+
					"use of the updateHandleCache method.");
		} else {
			if ( !updatedHandles.isEmpty() ) {
				for ( String windowId : updatedHandles ) {
					if ( !windowId.equals( mainHandle ) ) { // for all windows except main window
						if ( !handleCache.contains( windowId) ) { // for child windows not in allHandles cache
							newHandle = windowId; // set value of newly found window handle						
							log.println("-- Open window handle: " + newHandle + " (new window)" );
						}
					}
				}
				if ( !newHandle.equals("") ) { // outside loop so it catches latest window handle if there are multiple
					log.println("Switch to new window.");
					driver.switchTo().window( newHandle ); // switch to new window handle
				}
			} else {
				mainHandle = "";
				throw new IllegalStateException("No browser window handles are open.");
			}
		}
		handleCache = updatedHandles; // updates remembered set of open windows
		return newHandle;
	}

	public static void updateHandleCache() {
		printHandles();
		Set<String> updatedHandles = driver.getWindowHandles();
		if ( !updatedHandles.isEmpty() ) {
			if ( updatedHandles.size() > handleCache.size() ) {
				log.println( "Window handle number increased to: " + updatedHandles.size() );
			} else if ( updatedHandles.size() == handleCache.size() ) {
				log.println( "Window handle number is unchanged from: " + updatedHandles.size() );
			} else {
				log.println( "Window handle number decreased to: " + updatedHandles.size() );
			}			
		} else {
			mainHandle = null;
			throw new IllegalStateException("No browser window handles are open.");
		}
		handleCache = updatedHandles; // updates remembered set of open windows
	}

	public static void printHandles() {
		log.println( "Open windows:" );
		for ( String windowId : handleCache ) {
			log.print( "-- Open window handle: " + windowId );
			if ( windowId.equals( mainHandle ) ) {
				log.println(" (main handle)");
			} else {
				log.println();
			}
		}
	}

	public static void initializeStandaloneBrowser( String type ) {
		if ( type.equalsIgnoreCase( "firefox" ) ) {
			driver = new FirefoxDriver();
		} else if ( type.equalsIgnoreCase( "ie" ) ) {
			// ie driver server .exe needs to be in your system path
			driver = new InternetExplorerDriver();
		}
		driver.manage().timeouts().implicitlyWait( 30, TimeUnit.SECONDS );
		handleCache = driver.getWindowHandles();
		if ( handleCache.size() == 0 ) {
			mainHandle = "";
			throw new IllegalStateException("No browser window handles are open.\n" +
					"Browser is uninitialized.");
		} else if ( handleCache.size() > 1 ) {
			mainHandle = "";
			throw new IllegalStateException("More than one browser window handle is open.\n" +
					"Please close all browsers and restart test.");
		} else {
			mainHandle = driver.switchTo().defaultContent().getWindowHandle();
			mainWindowTitle = driver.switchTo().defaultContent().getTitle();
			setWindowPosition( mainHandle, 600, 800, 700, 40 );
		}
	}

	public static void setWindowPosition(String handle, int width, int height, int fleft, int ftop) {
		driver.switchTo().window( handle ).manage().window().setPosition( new Point(fleft, ftop) );
		driver.switchTo().window( handle ).manage().window().setSize( new Dimension( width, height) );
		//TODO add a javascript executor to get window focus
	}

	public static void waitTimer( int units, int mills ) {
		DecimalFormat df = new DecimalFormat("###.##");
		double totalSeconds = ((double)units*mills)/1000;
		log.println("Explicit pause for " + df.format(totalSeconds) + " seconds divided by " + units + " units of time: ");
		try {
			Thread.currentThread();		
			int x = 0;
			while( x < units ) {
				Thread.sleep( mills );
				log.print(".");
				x = x + 1;
			}
		} catch ( InterruptedException ex ) {
			ex.printStackTrace();
		}
		log.println(".");
	}

}