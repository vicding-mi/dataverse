package edu.harvard.iq.dataverse;

import org.hibernate.validator.constraints.UniqueElements;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotBlank;

import java.io.Serializable;
import java.sql.Timestamp;

@Entity
@Table(indexes = {@Index(columnList="id")})
public class FlexibleTerms extends DataverseEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    @UniqueElements
    @NotEmpty
    @Column(nullable = false)
    private String licenseId;

    @NotEmpty
    @Column(nullable = false)
    private String licenseDisplayName;

    // TODO: if can use licenseId as the value, then we do not need the following block
//    @NotEmpty
//    @Column(nullable = false)
//    private String value;

    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String licenseDescription;

    @NotNull
    @Column(nullable = false, columnDefinition = "text")
    private String licenseToolTip;


    public String getLicenseId() {
        return this.licenseId;
    }

    public void setLicenseId(String licenseId) {
        this.licenseId = licenseId;
    }


    /*
     * common functions
     */
    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FlexibleTerms)) {
            return false;
        }
        FlexibleTerms other = (FlexibleTerms) object;
        return !((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id)));
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dataverse.FlexibleTerms[ id=" + id + " ]";
    }

    /*
     * TODO: Defining relations to dataset and datafile??
     */
    @OneToOne(cascade={CascadeType.MERGE,CascadeType.PERSIST})
//    @JoinColumn(name="datafile_id", unique = true, nullable = false )
    private DataFile dataFile;

    public DataFile getDataFile() {
        return this.dataFile;
    }

    public void setDataFile(DataFile dataFile) {
        this.dataFile = dataFile;
    }






}
