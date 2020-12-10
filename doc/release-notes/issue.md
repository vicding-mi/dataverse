## Multiple licences feature proposal


## Summary

In the out-of-box Dataverse, when specifying the terms of use for a dataset you can only choose between applying the CC0 waiver or specifying custom terms. We propose to present the user with a list of standard licenses which replaces the Waiver radio button. CC0 and "custom license" will be in this list automatically. The other licences can be configured through a new API endpoint.


## Motivation

There are many other licenses than CC0 that are commonly used by depositors, for example CC-BY in several variants, In Dataverse depositors can only apply those terms by copy-pasting them into the "custom" terms field. This is not user friendly.


## Description of the changes


### User interface

The following screenshots show the current interface and the proposed interface.


#### Current interface


##### View mode
1. With 'Yes, apply CC0 - "Public Domain Dedication"'
<img width="780" alt="image2" src="https://user-images.githubusercontent.com/31689867/100609462-4f6c7a00-330e-11eb-85a5-b3b1ec2d684e.png">

2. With 'No, do not apply CC0 - "Public Domain Dedication"'
<img width="765" alt="image1" src="https://user-images.githubusercontent.com/31689867/100609553-7c209180-330e-11eb-8c0c-19b3c83dfad9.png">


##### Edit mode
3. With 'Yes, apply CC0 - "Public Domain Dedication"'
<img width="732" alt="image3" src="https://user-images.githubusercontent.com/31689867/100609589-8c387100-330e-11eb-9f0c-3e11c958ecd5.png">

4. With 'No, do not apply CC0 - "Public Domain Dedication"' 
![image6](https://user-images.githubusercontent.com/31689867/100609747-cbff5880-330e-11eb-9d0f-0c5e76a9e754.png)


#### Proposed interface


##### View mode
<img width="541" alt="image4" src="https://user-images.githubusercontent.com/31689867/100610218-9870fe00-330f-11eb-8a25-08265376831d.png">


##### Edit mode
1. With CC0 or other standard license selected
![image7](https://user-images.githubusercontent.com/31689867/100609766-d7528400-330e-11eb-954a-507f14b2576f.png)

2. With "Custom Terms" selected
![image5](https://user-images.githubusercontent.com/31689867/100610392-edad0f80-330f-11eb-8ffa-03ba669eb487.png)



### API


#### Configuration

Installing the list of available licenses is achieved by calling a new API endpoint:

_curl http://localhost:8080/api/admin/licenses/load -H "Content-type: text/tab-separated-values" -X POST --upload-file /tmp/licenses.tsv_

This uploads a configuration file that specifies the licenses that should appear in the dropdown box. This TSV file has the following columns:


<table>
  <tr>
   <td>licenseUri
   </td>
   <td>a unique identifier for the license (dropdown list value)
   </td>
  </tr>
  <tr>
   <td>licenseDisplayName
   </td>
   <td>the string to be displayed in the user interface (dropdown list text)
   </td>
  </tr>
  <tr>
   <td>licenseDescription
   </td>
   <td>a short description of the license to be displayed below the dropdown list
   </td>
  </tr>
</table>


There are two reserved items in the list, CC0 and "Custom Terms" which cannot be specified in the TSV file. They will always be present.


#### Metadata exports


##### (Native) JSON


###### Current output

When getting the JSON for a dataset the information about license currently looks like this:

For CC01:

{

      "id": 7,

	….

      "license": "CC0",

      "termsOfUse": "CC0 Waiver",

	…

      "termsOfAccess": "You need to request for access.",

}

For custom:

{

      "id": 7,

	….

      "license": "NONE",

	…

      "termsOfAccess": "You need to request for access.",

}


###### Proposed output

The current JSON will be extended with one field 'licenseURI'. The existing fields will be filled as follows:


<table>
  <tr>
   <td>license
   </td>
   <td>display name of the selected license
   </td>
  </tr>
  <tr>
   <td>termsOfUser
   </td>
   <td>description of the selected license
   </td>
  </tr>
</table>


For CC01:

{

      "id": 7,

	….

      "license": "CC0", 

      "termsOfUse": "CC0 Waiver",

	…

      "termsOfAccess": "You need to request for access.",

}

For other standard license (i.e CC-BY):

{

      "id": 7,

	….

**      "licenseURI": "https://creativecommons.org/licenses/by/4.0/legalcode"**,             

      "license": "CC-BY",

	...

      "termsOfAccess": "You need to request for access.",

}

For custom license:

{

      "id": 7,

	….

**      "licenseURI": "https://uro_to_custom_license.com/"**,             

      "license": "NONE",

      “termsOfUse”: “User input terms of use”

	...

      "termsOfAccess": "You need to request for access.",

}


##### Formats based on native JSON

The following formats seem to be based on the native JSON so the transformation will not have to be changed: Dublin Core, DataCite, DDI, DDI Codebook HTML.


##### OAI-ORE, JSON-LD, OpenAIRE

These formats currently contain a license URI for CC0. This URI will need to be filled in correctly for the other licenses as well.


### Migration

To support this functionality one extra table is required to store the list of licenses. Probably this table can be created automatically on deployment of the new Dataverse version that contains this new feature. This would mean that no migration scripts are necessary to activate this feature.
