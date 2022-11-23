package org.pygisserver.web.entity;

import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

public class Resource {

    String name;
    
    String uri;

    boolean published;
    
    
    /**
     * 工作空间
     */
    String workSpace;
    
    /**
     * 文件类型
     */
    String fileType;
    
    boolean multiCoverageReader;

    public void setPublished(boolean published) {
        this.published = published;
    }

    public Resource(Name name) {
        super();
        this.name = name.getLocalPart();
        this.uri = name.getNamespaceURI();
    }
    
    public Resource(String workSpace,String fileType)
    {
        
    }
    
    public String getLocalName() {
        return name;
    }

    public String getWorkSpace()
    {
        return workSpace;
    }

    public void setWorkSpace(String workSpace)
    {
        this.workSpace = workSpace;
    }

    public String getFileType()
    {
        return fileType;
    }

    public void setFileType(String fileType)
    {
        this.fileType = fileType;
    }

    public Name getName() {
        return new NameImpl(uri, name);
    }

    public boolean isPublished() {
        return published;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Resource other = (Resource) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public int compareTo(Resource o) {
        if (published && !o.published)
            return -1;
        else if (!published && o.published)
            return 1;
        return name.compareTo(o.name);
    }
    
    @Override
    public String toString() {
        return name + "(" + published + ")";
    }

    public boolean isMultiCoverageReader() {
        return multiCoverageReader;
    }

    public void setMultiCoverageReader(boolean multiCoverageReader) {
        this.multiCoverageReader = multiCoverageReader;
    }
}
