/*==========================================================================*\
 |  $Id$
 |*-------------------------------------------------------------------------*|
 |  Copyright (C) 2006 Virginia Tech
 |
 |  This file is part of Web-CAT.
 |
 |  Web-CAT is free software; you can redistribute it and/or modify
 |  it under the terms of the GNU General Public License as published by
 |  the Free Software Foundation; either version 2 of the License, or
 |  (at your option) any later version.
 |
 |  Web-CAT is distributed in the hope that it will be useful,
 |  but WITHOUT ANY WARRANTY; without even the implied warranty of
 |  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 |  GNU General Public License for more details. 
 |
 |  You should have received a copy of the GNU General Public License
 |  along with Web-CAT; if not, write to the Free Software
 |  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 |
 |  Project manager: Stephen Edwards <edwards@cs.vt.edu>
 |  Virginia Tech CS Dept, 660 McBryde Hall (0106), Blacksburg, VA 24061 USA
\*==========================================================================*/

package net.sf.webcat.birtruntime;

import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.birt.core.framework.Platform;
import org.eclipse.birt.report.engine.api.EngineConfig;
import org.eclipse.birt.report.engine.api.IReportEngine;
import org.eclipse.birt.report.engine.api.IReportEngineFactory;
import org.eclipse.birt.report.model.api.DesignConfig;
import org.eclipse.birt.report.model.api.IDesignEngine;
import org.eclipse.birt.report.model.api.IDesignEngineFactory;

import com.webobjects.eoaccess.EOUtilities;
import com.webobjects.eocontrol.EOEditingContext;
import com.webobjects.foundation.NSArray;
import com.webobjects.foundation.NSBundle;
import com.webobjects.foundation.NSData;
import com.webobjects.foundation.NSMutableArray;

import net.sf.webcat.core.Application;
import net.sf.webcat.core.Subsystem;

//-------------------------------------------------------------------------
/**
 *  This a completely empty class that simply ensures that the ExternalJars
 *  framework has a jar file created for it, and its resources end up being
 *  included as a live framework by various parts of WebObjects.
 *
 *  @author  stedwar2
 *  @version $Id$
 */
public class BIRTRuntime extends Subsystem
{
    //~ Constructors ..........................................................

    // ----------------------------------------------------------
    /**
     * Creates a new BIRTRuntime subsystem object.
     */
    public BIRTRuntime()
    {
        super();

        instance = this;
    }

    
    // ----------------------------------------------------------
    /**
     * Returns the sole instance of the BIRT runtime subsystem.
     *
     * @return the BIRTRuntime object that represents the subsystem.
     */
    public static BIRTRuntime getInstance()
    {
        return instance;
    }
    

    // ----------------------------------------------------------
    /* (non-Javadoc)
     * @see net.sf.webcat.core.Subsystem#init()
     */
    public void init()
    {
        super.init();

        initializeBIRT();
    }


    // ----------------------------------------------------------
    /**
     * Initializes the BIRT reporting engine.
     */
    private void initializeBIRT()
    {
        // Initialize the BIRT reporting engine.

        String reportEnginePath = myResourcesDir() + "/" +
            REPORT_ENGINE_SUBDIR;

        log.info("Using reporting engine located at " + reportEnginePath);

        EngineConfig config = new EngineConfig();
        config.setEngineHome( reportEnginePath );

        DesignConfig dConfig = new DesignConfig();
        dConfig.setBIRTHome( reportEnginePath );

        // Point the OSGi platform's configuration area to the storage folder
        // chosen by the Web-CAT admin. Otherwise, the default location is in
        // the report engine path specified above, which could be read-only.

        String configArea = net.sf.webcat.core.Application
            .configurationProperties().getProperty("grader.submissiondir") +
            "/ReporterConfiguration";
        String instanceArea = net.sf.webcat.core.Application
            .configurationProperties().getProperty("grader.submissiondir") +
            "/ReporterWorkspace";

        // Copy the initial config area files from the ReportEngine subfolder
        // into the new config area if they aren't already there.

        File configAreaDir = new File(configArea);
        if(!configAreaDir.exists())
        {
            configAreaDir.mkdirs();

            File configSrcDir = new File(reportEnginePath + "/configuration");

            try
            {
                net.sf.webcat.archives.FileUtilities
                    .copyDirectoryContentsIfNecessary(configSrcDir,
                            configAreaDir);
            }
            catch (IOException e)
            {
                log.fatal("Could not copy BIRT configuration data into " +
                        "Web-CAT storage location", e);
            }
        }

        Map osgiConfig = new Hashtable();
        osgiConfig.put("osgi.configuration.area", configArea);
        osgiConfig.put("osgi.instance.area", instanceArea);
        config.setOSGiConfig(osgiConfig);
        dConfig.setOSGiConfig(osgiConfig);

        try
        {
            Platform.startup( config );

            IReportEngineFactory factory = (IReportEngineFactory) Platform
                .createFactoryObject(
                        IReportEngineFactory.EXTENSION_REPORT_ENGINE_FACTORY );

            reportEngine = factory.createReportEngine( config );

            IDesignEngineFactory dFactory = (IDesignEngineFactory) Platform
            .createFactoryObject(
                    IDesignEngineFactory.EXTENSION_DESIGN_ENGINE_FACTORY );

            designEngine = dFactory.createDesignEngine( dConfig );
        }
        catch (Exception e)
        {
            log.fatal("Error initializing BIRT reporting engine", e);
        }
    }


    // ----------------------------------------------------------
    /**
     * Gets the reference to the BIRT report engine.
     * 
     * @return a reference to the BIRT report engine.
     */
    public IReportEngine getReportEngine()
    {
        return reportEngine;
    }
    

    // ----------------------------------------------------------
    /**
     * Gets the reference to the BIRT design engine.
     * 
     * @return a reference to the BIRT design engine.
     */
    public IDesignEngine getDesignEngine()
    {
        return designEngine;
    }

    
    //~ Instance Variables ....................................................

    /**
     * This is the sole instance of the BIRT runtime subsystem, initialized by
     * the constructor.
     */
    private static BIRTRuntime instance;

    /**
     * This is the sole instance of the report engine.
     */
    private IReportEngine reportEngine;

    /**
     * This is the sole instance of the report design engine, used to traverse
     * the report template files as they are uploaded to extract various useful
     * information.
     */
    private IDesignEngine designEngine;
    
    private static Logger log = Logger.getLogger( BIRTRuntime.class );

    private static final String REPORT_ENGINE_SUBDIR = "ReportEngine";

}