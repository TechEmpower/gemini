/*******************************************************************************
 * Copyright (c) 2018, TechEmpower, Inc.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name TechEmpower, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived from
 *       this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL TECHEMPOWER, INC. BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/

package com.techempower.gemini.internationalization;

import java.util.*;

import com.techempower.gemini.*;
import com.techempower.util.*;

/**
 * Manages a <b>default</b> set of properties for Gemini.  If a Gemini-based
 * application is not using Gemini's locale management facilities, this
 * default set of properties will be used exclusively.  In an localized
 * application, however, it would not be typical to use this set of properties
 * because it will contain no resources that are specific to the application
 * in question--it only contains resources for Gemini itself (Form Element
 * validation instructions, etc.).
 *   <p>
 * The Gemini naming convention for resources is: 
 *   g<lowercase-class-initials>-<...>.
 *   <p>
 * That is, for an resource named "val1" from FormTextArea (fta), the fully 
 * qualified name of the resource is "gfta-val1".
 */
public class DefaultGeminiResources
  extends    GeminiResources
{
  //
  // Member variables.
  //
  
  private List<Country> countries = new ArrayList<>(200);

  //
  // Member methods.
  //

  /**
   * Default constructor.
   */
  public DefaultGeminiResources(GeminiApplication application)
  {
    super(application);

    constructFormElementResources();
  }

  /**
   * Creates the resources for FormElements.
   */
  protected void constructFormElementResources()
  {
    Properties properties = getAll();
    
    // FormTextField (ftf)
    properties.put("gftf-empty-e", "$S1 is empty.");
    properties.put("gftf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gftf-empty-l", "Please provide input in this field.");
    properties.put("gftf-short-e", "$S1 is too short.");
    properties.put("gftf-short-i", "Please specify at least $S2 characters in the field named $S1.");
    properties.put("gftf-short-l", "Please specify at least $S2 characters.");
    properties.put("gftf-long-e", "$S1 is too long.");
    properties.put("gftf-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gftf-long-l", "Please keep the input in this field to $S2 characters or less.");

    // FormCheckBox (fcb)

    properties.put("gfcb-unchecked-e", "$S1 is unchecked.");
    properties.put("gfcb-unchecked-i", "Please check the checkbox named $S1.");
    properties.put("gfcb-unchecked-l", "This checkbox field must be checked.");

    // FormTextArea (fta)

    properties.put("gfta-empty-e", "$S1 is empty.");
    properties.put("gfta-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfta-empty-l", "Please provide input in this field.");
    properties.put("gfta-short-e", "$S1 is too short.");
    properties.put("gfta-short-i", "Please specify at least $S2 characters in the field named $S1.");
    properties.put("gfta-short-l", "Please specify at least $S2 characters.");
    properties.put("gfta-long-e", "$S1 is too long.");
    properties.put("gfta-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gfta-long-l", "Please keep the input in this field to $S2 characters or less.");

    // FormIntegerField (fif)

    properties.put("gfif-low-e", "$S1 is lower than $S2.");
    properties.put("gfif-low-i", "$S1 cannot be less than $S2.");
    properties.put("gfif-low-l", "This field's value cannot be less than $S2.");
    properties.put("gfif-high-e", "$S1 is higher than $S2.");
    properties.put("gfif-high-i", "$S1 cannot be greater than $S2.");
    properties.put("gfif-high-l", "This field's value cannot be greater than $S2.");
    properties.put("gfif-invalid-e", "$S1 does not contain a valid integer value.");
    properties.put("gfif-invalid-i", "$S1 requires a valid integer value to be entered.");
    properties.put("gfif-invalid-l", "\"$S2\" is not a valid integer.");

    // FormUsernameField (fuf)

    properties.put("gfuf-short-e", "$S1 is too short ($S2 character minimum).");
    properties.put("gfuf-short-i", "$S1 cannot be shorter than $S2 characters.");
    properties.put("gfuf-short-l", "This field's length cannot be less than $S2 characters.");
    properties.put("gfuf-badchars-e", "$S1 contains bad characters.");
    properties.put("gfuf-badchars-i", "$S1 is improperly formatted or contains characters that are not permitted for usernames.");
    properties.put("gfuf-badchars-l", "This field contains unsupported characters or is improperly formatted.");

    // FormFloatField (fff)

    properties.put("gfff-low-e", "$S1 is lower than $S2.");
    properties.put("gfff-low-i", "$S1 cannot be less than $S2.");
    properties.put("gfff-low-l", "This field's value cannot be less than $S2.");
    properties.put("gfff-high-e", "$S1 is higher than $S2.");
    properties.put("gfff-high-i", "$S1 cannot be greater than $S2.");
    properties.put("gfff-high-l", "This field's value cannot be greater than $S2.");
    properties.put("gfff-invalid-e", "$S1 does not contain a valid decimal/floating point value.");
    properties.put("gfff-invalid-i", "$S1 requires a valid decimal/floating point value to be entered.");
    properties.put("gfff-invalid-l", "\"$S2\" is not a valid decimal/floating point value.");

    // FormRadioButtonGroup (frbg)

    properties.put("gfrbg-empty-e", "$S1 is unselected.");
    properties.put("gfrbg-empty-i", "Please provide input in the area named $S1.");
    properties.put("gfrbg-empty-l", "Please provide input in this field.");
    properties.put("gfrbg-invalid-e", "$S1 value is invalid.");
    properties.put("gfrbg-invalid-i", "The selection in the area named $S1 is invalid.");
    properties.put("gfrbg-invalid-l", "This selection is invalid.");

    // FormMultiSelectBox (fmsb)

    properties.put("gfmsb-lowsingular-e", "$S1 does not have an option selected.");
    properties.put("gfmsb-lowsingular-i", "Please select 1 option from the multi-select box named $S1.");
    properties.put("gfmsb-lowsingular-l", "This multi-select box requires 1 selection.");
    properties.put("gfmsb-lowplural-e", "$S1 does not have enough options selected.");
    properties.put("gfmsb-lowplural-i", "Please select at least $S2 options from the multi-select box named $S1.");
    properties.put("gfmsb-lowplural-l", "This multi-select box requires $S2 selections.");
    properties.put("gfmsb-highsingular-e", "$S1 is too many selections.");
    properties.put("gfmsb-highsingular-i", "Please limit yourself to 1 selection from the multi-select box named $S1.");
    properties.put("gfmsb-highsingular-l", "This multi-select box is limited to 1 selection.");
    properties.put("gfmsb-highplural-e", "$S1 is too many selections.");
    properties.put("gfmsb-highplural-i", "Please limit yourself to $S3 selections from the multi-select box named $S1.");
    properties.put("gfmsb-highplural-l", "This multi-select box is limited to $S3 selections.");

    // FormSelect (fs)

    properties.put("gfs-empty-e", "$S1 is unselected.");
    properties.put("gfs-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfs-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfs-invalid-e", "$S1 value is invalid.");
    properties.put("gfs-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfs-invalid-l", "This selection is invalid.");

    // FormDropDownMenu (fddm)

    properties.put("gfddm-empty-e", "$S1 is unselected.");
    properties.put("gfddm-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddm-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddm-invalid-e", "$S1 value is invalid.");
    properties.put("gfddm-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddm-invalid-l", "This selection is invalid.");
    
    // FormDropDownMenuPopulator (fddmp)

    properties.put("gfddmp-empty-e", "$S1 is unselected.");
    properties.put("gfddmp-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddmp-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddmp-invalid-e", "$S1 value is invalid.");
    properties.put("gfddmp-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddmp-invalid-l", "This selection is invalid.");

    // FromDropDownMenuCountry (fddmc)
    
    this.countries.add(new Country("AF", "Afghanistan"));
    this.countries.add(new Country("AL", "Albania"));
    this.countries.add(new Country("DZ", "Algeria"));
    this.countries.add(new Country("AS", "American Samoa"));
    this.countries.add(new Country("AD", "Andorra, Principality of"));
    this.countries.add(new Country("AO", "Angola"));
    this.countries.add(new Country("AI", "Anguilla"));
    this.countries.add(new Country("AQ", "Antarctica"));
    this.countries.add(new Country("AG", "Antigua and Barbuda"));
    this.countries.add(new Country("AR", "Argentina"));
    this.countries.add(new Country("AM", "Armenia"));
    this.countries.add(new Country("AW", "Aruba"));
    this.countries.add(new Country("AU", "Australia"));
    this.countries.add(new Country("AT", "Austria"));
    this.countries.add(new Country("AZ", "Azerbaidjan"));
    this.countries.add(new Country("BS", "Bahamas"));
    this.countries.add(new Country("BH", "Bahrain"));
    this.countries.add(new Country("BD", "Bangladesh"));
    this.countries.add(new Country("BB", "Barbados"));
    this.countries.add(new Country("BY", "Belarus"));
    this.countries.add(new Country("BE", "Belgium"));
    this.countries.add(new Country("BZ", "Belize"));
    this.countries.add(new Country("BJ", "Benin"));
    this.countries.add(new Country("BM", "Bermuda"));
    this.countries.add(new Country("BT", "Bhutan"));
    this.countries.add(new Country("BO", "Bolivia"));
    this.countries.add(new Country("BA", "Bosnia-Herzegovina"));
    this.countries.add(new Country("BW", "Botswana"));
    this.countries.add(new Country("BV", "Bouvet Island"));
    this.countries.add(new Country("BR", "Brazil"));
    this.countries.add(new Country("IO", "British Indian Ocean Territory"));
    this.countries.add(new Country("BN", "Brunei Darussalam"));
    this.countries.add(new Country("BG", "Bulgaria"));
    this.countries.add(new Country("BF", "Burkina Faso"));
    this.countries.add(new Country("BI", "Burundi"));
    this.countries.add(new Country("KH", "Cambodia, Kingdom of"));
    this.countries.add(new Country("CM", "Cameroon"));
    this.countries.add(new Country("CA", "Canada"));
    this.countries.add(new Country("CV", "Cape Verde"));
    this.countries.add(new Country("KY", "Cayman Islands"));
    this.countries.add(new Country("CF", "Central African Republic"));
    this.countries.add(new Country("TD", "Chad"));
    this.countries.add(new Country("CL", "Chile"));
    this.countries.add(new Country("CN", "China"));
    this.countries.add(new Country("CX", "Christmas Island"));
    this.countries.add(new Country("CC", "Cocos (Keeling) Islands"));
    this.countries.add(new Country("CO", "Colombia"));
    this.countries.add(new Country("KM", "Comoros"));
    this.countries.add(new Country("CG", "Congo"));
    this.countries.add(new Country("CD", "Congo, The Democratic Republic of the"));
    this.countries.add(new Country("CK", "Cook Islands"));
    this.countries.add(new Country("CR", "Costa Rica"));
    this.countries.add(new Country("HR", "Croatia"));
    this.countries.add(new Country("CU", "Cuba"));
    this.countries.add(new Country("CY", "Cyprus"));
    this.countries.add(new Country("CZ", "Czech Republic"));
    this.countries.add(new Country("DK", "Denmark"));
    this.countries.add(new Country("DJ", "Djibouti"));
    this.countries.add(new Country("DM", "Dominica"));
    this.countries.add(new Country("DO", "Dominican Republic"));
    this.countries.add(new Country("TP", "East Timor"));
    this.countries.add(new Country("EC", "Ecuador"));
    this.countries.add(new Country("EG", "Egypt"));
    this.countries.add(new Country("SV", "El Salvador"));
    this.countries.add(new Country("GQ", "Equatorial Guinea"));
    this.countries.add(new Country("ER", "Eritrea"));
    this.countries.add(new Country("EE", "Estonia"));
    this.countries.add(new Country("ET", "Ethiopia"));
    this.countries.add(new Country("FK", "Falkland Islands"));
    this.countries.add(new Country("FO", "Faroe Islands"));
    this.countries.add(new Country("FJ", "Fiji"));
    this.countries.add(new Country("FI", "Finland"));
    this.countries.add(new Country("CS", "Former Czechoslovakia"));
    this.countries.add(new Country("SU", "Former USSR"));
    this.countries.add(new Country("FR", "France"));
    this.countries.add(new Country("FX", "France (European Territory)"));
    this.countries.add(new Country("GF", "French Guyana"));
    this.countries.add(new Country("TF", "French Southern Territories"));
    this.countries.add(new Country("GA", "Gabon"));
    this.countries.add(new Country("GM", "Gambia"));
    this.countries.add(new Country("GE", "Georgia"));
    this.countries.add(new Country("DE", "Germany"));
    this.countries.add(new Country("GH", "Ghana"));
    this.countries.add(new Country("GI", "Gibraltar"));
    this.countries.add(new Country("GB", "Great Britain"));
    this.countries.add(new Country("GR", "Greece"));
    this.countries.add(new Country("GL", "Greenland"));
    this.countries.add(new Country("GD", "Grenada"));
    this.countries.add(new Country("GP", "Guadeloupe (French)"));
    this.countries.add(new Country("GU", "Guam (USA)"));
    this.countries.add(new Country("GT", "Guatemala"));
    this.countries.add(new Country("GN", "Guinea"));
    this.countries.add(new Country("GW", "Guinea Bissau"));
    this.countries.add(new Country("GY", "Guyana"));
    this.countries.add(new Country("HT", "Haiti"));
    this.countries.add(new Country("HM", "Heard and McDonald Islands"));
    this.countries.add(new Country("VA", "Holy See (Vatican City State)"));
    this.countries.add(new Country("HN", "Honduras"));
    this.countries.add(new Country("HK", "Hong Kong"));
    this.countries.add(new Country("HU", "Hungary"));
    this.countries.add(new Country("IS", "Iceland"));
    this.countries.add(new Country("IN", "India"));
    this.countries.add(new Country("ID", "Indonesia"));
    this.countries.add(new Country("IR", "Iran"));
    this.countries.add(new Country("IQ", "Iraq"));
    this.countries.add(new Country("IE", "Ireland"));
    this.countries.add(new Country("IL", "Israel"));
    this.countries.add(new Country("IT", "Italy"));
    this.countries.add(new Country("CI", "Ivory Coast (Cote D'Ivoire)"));
    this.countries.add(new Country("JM", "Jamaica"));
    this.countries.add(new Country("JP", "Japan"));
    this.countries.add(new Country("JO", "Jordan"));
    this.countries.add(new Country("KZ", "Kazakhstan"));
    this.countries.add(new Country("KE", "Kenya"));
    this.countries.add(new Country("KI", "Kiribati"));
    this.countries.add(new Country("KW", "Kuwait"));
    this.countries.add(new Country("KG", "Kyrgyz Republic (Kyrgyzstan)"));
    this.countries.add(new Country("LA", "Laos"));
    this.countries.add(new Country("LV", "Latvia"));
    this.countries.add(new Country("LB", "Lebanon"));
    this.countries.add(new Country("LS", "Lesotho"));
    this.countries.add(new Country("LR", "Liberia"));
    this.countries.add(new Country("LY", "Libya"));
    this.countries.add(new Country("LI", "Liechtenstein"));
    this.countries.add(new Country("LT", "Lithuania"));
    this.countries.add(new Country("LU", "Luxembourg"));
    this.countries.add(new Country("MO", "Macau"));
    this.countries.add(new Country("MK", "Macedonia"));
    this.countries.add(new Country("MG", "Madagascar"));
    this.countries.add(new Country("MW", "Malawi"));
    this.countries.add(new Country("MY", "Malaysia"));
    this.countries.add(new Country("MV", "Maldives"));
    this.countries.add(new Country("ML", "Mali"));
    this.countries.add(new Country("MT", "Malta"));
    this.countries.add(new Country("MH", "Marshall Islands"));
    this.countries.add(new Country("MQ", "Martinique (French)"));
    this.countries.add(new Country("MR", "Mauritania"));
    this.countries.add(new Country("MU", "Mauritius"));
    this.countries.add(new Country("YT", "Mayotte"));
    this.countries.add(new Country("MX", "Mexico"));
    this.countries.add(new Country("FM", "Micronesia"));
    this.countries.add(new Country("MD", "Moldavia"));
    this.countries.add(new Country("MC", "Monaco"));
    this.countries.add(new Country("MN", "Mongolia"));
    this.countries.add(new Country("MS", "Montserrat"));
    this.countries.add(new Country("MA", "Morocco"));
    this.countries.add(new Country("MZ", "Mozambique"));
    this.countries.add(new Country("MM", "Myanmar"));
    this.countries.add(new Country("NA", "Namibia"));
    this.countries.add(new Country("NR", "Nauru"));
    this.countries.add(new Country("NP", "Nepal"));
    this.countries.add(new Country("NL", "Netherlands"));
    this.countries.add(new Country("AN", "Netherlands Antilles"));
    this.countries.add(new Country("NT", "Neutral Zone"));
    this.countries.add(new Country("NC", "New Caledonia (French)"));
    this.countries.add(new Country("NZ", "New Zealand"));
    this.countries.add(new Country("NI", "Nicaragua"));
    this.countries.add(new Country("NE", "Niger"));
    this.countries.add(new Country("NG", "Nigeria"));
    this.countries.add(new Country("NU", "Niue"));
    this.countries.add(new Country("NF", "Norfolk Island"));
    this.countries.add(new Country("KP", "North Korea"));
    this.countries.add(new Country("MP", "Northern Mariana Islands"));
    this.countries.add(new Country("NO", "Norway"));
    this.countries.add(new Country("OM", "Oman"));
    this.countries.add(new Country("PK", "Pakistan"));
    this.countries.add(new Country("PW", "Palau"));
    this.countries.add(new Country("PA", "Panama"));
    this.countries.add(new Country("PG", "Papua New Guinea"));
    this.countries.add(new Country("PY", "Paraguay"));
    this.countries.add(new Country("PE", "Peru"));
    this.countries.add(new Country("PH", "Philippines"));
    this.countries.add(new Country("PN", "Pitcairn Island"));
    this.countries.add(new Country("PL", "Poland"));
    this.countries.add(new Country("PF", "Polynesia (French)"));
    this.countries.add(new Country("PT", "Portugal"));
    this.countries.add(new Country("PR", "Puerto Rico"));
    this.countries.add(new Country("QA", "Qatar"));
    this.countries.add(new Country("RE", "Reunion (French)"));
    this.countries.add(new Country("RO", "Romania"));
    this.countries.add(new Country("RU", "Russian Federation"));
    this.countries.add(new Country("RW", "Rwanda"));
    this.countries.add(new Country("GS", "S. Georgia & S. Sandwich Isls."));
    this.countries.add(new Country("SH", "Saint Helena"));
    this.countries.add(new Country("KN", "Saint Kitts & Nevis Anguilla"));
    this.countries.add(new Country("LC", "Saint Lucia"));
    this.countries.add(new Country("PM", "Saint Pierre and Miquelon"));
    this.countries.add(new Country("ST", "Saint Tome (Sao Tome) and Principe"));
    this.countries.add(new Country("VC", "Saint Vincent & Grenadines"));
    this.countries.add(new Country("WS", "Samoa"));
    this.countries.add(new Country("SM", "San Marino"));
    this.countries.add(new Country("SA", "Saudi Arabia"));
    this.countries.add(new Country("SN", "Senegal"));
    this.countries.add(new Country("SC", "Seychelles"));
    this.countries.add(new Country("SL", "Sierra Leone"));
    this.countries.add(new Country("SG", "Singapore"));
    this.countries.add(new Country("SK", "Slovak Republic"));
    this.countries.add(new Country("SI", "Slovenia"));
    this.countries.add(new Country("SB", "Solomon Islands"));
    this.countries.add(new Country("SO", "Somalia"));
    this.countries.add(new Country("ZA", "South Africa"));
    this.countries.add(new Country("KR", "South Korea"));
    this.countries.add(new Country("ES", "Spain"));
    this.countries.add(new Country("LK", "Sri Lanka"));
    this.countries.add(new Country("SD", "Sudan"));
    this.countries.add(new Country("SR", "Suriname"));
    this.countries.add(new Country("SJ", "Svalbard and Jan Mayen Islands"));
    this.countries.add(new Country("SZ", "Swaziland"));
    this.countries.add(new Country("SE", "Sweden"));
    this.countries.add(new Country("CH", "Switzerland"));
    this.countries.add(new Country("SY", "Syria"));
    this.countries.add(new Country("TJ", "Tadjikistan"));
    this.countries.add(new Country("TW", "Taiwan"));
    this.countries.add(new Country("TZ", "Tanzania"));
    this.countries.add(new Country("TH", "Thailand"));
    this.countries.add(new Country("TG", "Togo"));
    this.countries.add(new Country("TK", "Tokelau"));
    this.countries.add(new Country("TO", "Tonga"));
    this.countries.add(new Country("TT", "Trinidad and Tobago"));
    this.countries.add(new Country("TN", "Tunisia"));
    this.countries.add(new Country("TR", "Turkey"));
    this.countries.add(new Country("TM", "Turkmenistan"));
    this.countries.add(new Country("TC", "Turks and Caicos Islands"));
    this.countries.add(new Country("TV", "Tuvalu"));
    this.countries.add(new Country("UG", "Uganda"));
    this.countries.add(new Country("UA", "Ukraine"));
    this.countries.add(new Country("AE", "United Arab Emirates"));
    this.countries.add(new Country("UK", "United Kingdom"));
    this.countries.add(new Country("US", "United States"));
    this.countries.add(new Country("UY", "Uruguay"));
    this.countries.add(new Country("UM", "USA Minor Outlying Islands"));
    this.countries.add(new Country("UZ", "Uzbekistan"));
    this.countries.add(new Country("VU", "Vanuatu"));
    this.countries.add(new Country("VE", "Venezuela"));
    this.countries.add(new Country("VN", "Vietnam"));
    this.countries.add(new Country("VG", "Virgin Islands (British)"));
    this.countries.add(new Country("VI", "Virgin Islands (USA)"));
    this.countries.add(new Country("WF", "Wallis and Futuna Islands"));
    this.countries.add(new Country("EH", "Western Sahara"));
    this.countries.add(new Country("YE", "Yemen"));
    this.countries.add(new Country("YU", "Yugoslavia"));
    this.countries.add(new Country("ZR", "Zaire"));
    this.countries.add(new Country("ZM", "Zambia"));
    this.countries.add(new Country("ZW", "Zimbabwe"));

    StringList fddmcValue = new StringList();
    StringList fddmcDisplay = new StringList("#");
    Iterator<Country> countryIter = this.countries.iterator();
    Country country;
    while (countryIter.hasNext())
    {
      country = countryIter.next();
      addCountryToLists(country, fddmcValue, fddmcDisplay);
    }
    
    properties.put("gfddmc-value", fddmcValue.toString()); 
    properties.put("gfddmc-display", fddmcDisplay.toString());
    
    properties.put("gfddmc-empty-e", "$S1 is unselected.");
    properties.put("gfddmc-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddmc-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddmc-invalid-e", "$S1 value is invalid.");
    properties.put("gfddmc-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddmc-invalid-l", "This selection is invalid.");
    
    // FromDropDownMenuCountryUsFirst (fddmcusf)
    
    StringList fddmcusfValue = new StringList();
    StringList fddmcusfDisplay = new StringList("#");
    
    // Grab the US first.
    ArrayList<Country> lcfCountries = new ArrayList<>(this.countries);
    country = lcfCountries.get(228);  // United States
    addCountryToLists(country, fddmcusfValue, fddmcusfDisplay);
    lcfCountries.remove(228);
    
    // Add the rest of the countries.
    countryIter = lcfCountries.iterator();
    while (countryIter.hasNext())
    {
      country = countryIter.next();
      addCountryToLists(country, fddmcusfValue, fddmcusfDisplay);
    } 
    
    properties.put("gfddmcusf-value", fddmcusfValue.toString()); 
    properties.put("gfddmcusf-display", fddmcusfDisplay.toString());
    
    properties.put("gfddmcusf-empty-e", "$S1 is unselected.");
    properties.put("gfddmcusf-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddmcusf-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddmcusf-invalid-e", "$S1 value is invalid.");
    properties.put("gfddmcusf-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddmcusf-invalid-l", "This selection is invalid.");

    // FromDropDownMenuMonth (fddmm)
    
    properties.put("gfddmm-value", "1,2,3,4,5,6,7,8,9,10,11,12");
    properties.put("gfddmm-display", "January#February#March#April#May#June#July#August#September#"
      + "October#November#December#");
    
    properties.put("gfddmm-empty-e", "$S1 is unselected.");
    properties.put("gfddmm-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddmm-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddmm-invalid-e", "$S1 value is invalid.");
    properties.put("gfddmm-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddmm-invalid-l", "This selection is invalid.");
    
    // FromDropDownMenuState (fddms)
    
    properties.put("gfddms-value", "AL,AK,AZ,AR,CA,CO,CT,DE,DC,FL,GA,HI,ID,IL,IN,IA,KS,"
	    + "KY,LA,ME,MD,MA,MI,MN,MS,MO,MT,NE,NV,NH,NJ,NM,NY,NC,ND,OH,OK,OR,PA,RI,SC,SD,TN,"
	    + "TX,UT,VT,VA,WA,WV,WI,WY");
    properties.put("gfddms-display", "Alabama#Alaska#Arizona#Arkansas#California#"
      + "Colorado#Connecticut#Delaware#District of Columbia#Florida#Georgia#Hawaii#Idaho#Illinois#Indiana#"
      + "Iowa#Kansas#Kentucky#Louisiana#Maine#Maryland#Massachusetts#Michigan#Minnesota#"
      + "Mississippi#Missouri#Montana#Nebraska#Nevada#New Hampshire#New Jersey#New Mexico#"
      + "New York#North Carolina#North Dakota#Ohio#Oklahoma#Oregon#Pennsylvania#Rhode Island#"
      + "South Carolina#South Dakota#Tennessee#Texas#Utah#Vermont#Virginia#Washington#"
      + "West Virginia#Wisconsin#Wyoming#");
    
    properties.put("gfddms-empty-e", "$S1 is unselected.");
    properties.put("gfddms-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddms-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddms-invalid-e", "$S1 value is invalid.");
    properties.put("gfddms-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddms-invalid-l", "This selection is invalid.");
    
    // FromDropDownMenuState+Canada (fddms+c)
    
    properties.put("gfddms+c-value", "AL,AK,AB,AZ,AR,BC,CA,CO,CT,DE,DC,FL,GA,HI,ID,IL,IN,IA,KS,"
      + "KY,LA,ME,MB,MD,MA,MI,MN,MS,MO,MT,NE,NV,NB,NF,NH,NJ,NM,NY,NC,ND,NS,NU,OH,ON,OK,OR,PA,PE,QC,RI,SK,SC,SD,TN,"
      + "TX,UT,VT,VA,WA,WV,WI,WY,YK");
    properties.put("gfddms+c-display", "Alabama#Alaska#Alberta#Arizona#Arkansas#British Columbia#California#"
      + "Colorado#Connecticut#Delaware#District of Columbia#Florida#Georgia#Hawaii#Idaho#Illinois#Indiana#"
      + "Iowa#Kansas#Kentucky#Louisiana#Maine#Manitoba#Maryland#Massachusetts#Michigan#Minnesota#"
      + "Mississippi#Missouri#Montana#Nebraska#Nevada#New Brunswick#Newfoundland#New Hampshire#New Jersey#New Mexico#"
      + "New York#North Carolina#North Dakota#Nova Scotia#Nunavut#Ohio#Ontario#Oklahoma#Oregon#Pennsylvania#Prince Edward Island#Quebec#Rhode Island#"
      + "Saskatchewan#South Carolina#South Dakota#Tennessee#Texas#Utah#Vermont#Virginia#Washington#"
      + "West Virginia#Wisconsin#Wyoming#Yukon#");
    
    // Alberta - AB
    // British Columbia - BC
    // Manitoba - MB
    // New Brunswick - NB
    // Newfoundland - NF
    // Northwest Territories - NT
    // Nova Scotia - NS
    // Nunavut - NU
    // Ontario - ON
    // Prince Edward Island - PE
    // Quebec - QC
    // Saskatchewan - SK
    // Yukon - YK
    
    properties.put("gfddms+c-empty-e", "$S1 is unselected.");
    properties.put("gfddms+c-empty-i", "Please make a selection in the drop-down menu named $S1.");
    properties.put("gfddms+c-empty-l", "This drop-down menu requires a selection.");
    properties.put("gfddms+c-invalid-e", "$S1 value is invalid.");
    properties.put("gfddms+c-invalid-i", "The selection in the drop-down menu named $S1 is invalid.");
    properties.put("gfddms+c-invalid-l", "This selection is invalid.");

    // FormFilePathField (ffpf)

    properties.put("gffpf-empty-e", "$S1 is empty.");
    properties.put("gffpf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gffpf-empty-l", "Please provide input in this field.");
    properties.put("gffpf-badchars-e", "$S1 contains one of the following invalid characters:  * : ? \" < > |");
    properties.put("gffpf-badchars-i", "$S1 may not contain any of the following characters:  * : ? \" < > |");
    properties.put("gffpf-badchars-l", "Please remove any of the following characters from your input:  * : ? \" < > |");
    properties.put("gffpf-format-e", "$S1 is not in an acceptable file path format.");
    properties.put("gffpf-format-i", "$S1 contains an incorrectly formatted file path.");
    properties.put("gffpf-format-l", "This field contains an incorrectly formatted file path.");
    properties.put("gffpf-short-e", "$S1 is too short to provide a meaningful file path.");
    properties.put("gffpf-short-i", "Please provide a longer value in the field named $S1.");
    properties.put("gffpf-short-l", "Please provide a longer value in this field.");

    // FormFileBrowse (ffb)

    properties.put("gffb-empty-e", "$S1 is empty.");
    properties.put("gffb-empty-i", "Please provide input in the area named $S1.");
    properties.put("gffb-empty-l", "Please provide input in this field.");
    
    
    // FormEnhancedFileBrowser (fefb)
    properties.put("gfefb-empty-e", "$S1 is too large.");
    properties.put("gfefb-empty-i", "Please keep the file under $S2 in the field named $S1.");
    properties.put("gfefb-empty-l", "Please keep the file under $S2.");
    
    properties.put("gfefb-invalid-e", "$S1 contains an invalid file type.");
    properties.put("gfefb-invalid-i", "$S1 must be one of the following types $S2.");
    properties.put("gfefb-invalid-l", "File must be one of the following types $S2.");
    
    properties.put("gfefb-error-e", "Unable to retrieve file for field named $S1.");
    properties.put("gfefb-error-i", "Unable to retrieve file for field named $S1 due to error.");
    properties.put("gfefb-error-l", "Unable to retrieve file for field named $S1 due to error.  Please try again.");
    

    // FormComplexDateField (fcdf)

    properties.put("gfcdf-empty-e", "$S1 is empty.");
    properties.put("gfcdf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfcdf-empty-l", "Please provide input in this field.");
    properties.put("gfcdf-before-e", "$S1 is earlier than $S2.");
    properties.put("gfcdf-before-i", "$S1 cannot be earlier than $S2.");
    properties.put("gfcdf-before-l", "This field's value cannot be earlier than $S2.");
    properties.put("gfcdf-after-e", "$S1 is later than $S2.");
    properties.put("gfcdf-after-i", "$S1 cannot be later than $S2.");
    properties.put("gfcdf-after-l", "This field's value cannot be later than $S2.");
    properties.put("gfcdf-invalid-e", "$S1 is not in a valid format.");
    properties.put("gfcdf-invalid-i", "$S1 contains an incorrectly formatted date.");
    properties.put("gfcdf-invalid-l", "This field's value is incorrectly formatted.");
    
    // FormDateField( fdf )
    
    properties.put("gfdf-empty-e", "$S1 is empty.");
    properties.put("gfdf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfdf-empty-l", "Please provide input in this field.");
    properties.put("gfdf-long-e", "$S1 is too long.");
    properties.put("gfdf-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gfdf-long-l", "Please keep the input in this field to $S2 characters or less.");
    properties.put("gfdf-invalidmonth-e", "$S1 has an invalid month ($S2-$S3).");
    properties.put("gfdf-invalidmonth-i", "$S1 has an invalid month.  Valid months are from $S2 to $S3.");
    properties.put("gfdf-invalidmonth-l", "This field's month must be in the range $S2 to $S3.");
    properties.put("gfdf-invalidyear-e", "$S1 has an invalid year ($S2-$S3).");
    properties.put("gfdf-invalidyear-i", "$S1 has an invalid year.  Valid years are from $S2-$S3.");
    properties.put("gfdf-invalidyear-l", "This field's year must be in the range $S2-$S3.");
    properties.put("gfdf-invalidday-e", "$S1 has an invalid day ($S2-$S3).");
    properties.put("gfdf-invalidday-i", "$S1 has an invalid day.  Valid days are from $S2-$S3.");
    properties.put("gfdf-invalidday-l", "This field's day must be in the range $S2-$S3.");
    properties.put("gfdf-invalidformat-e", "$S1 is incorrectly formatted.  Format: mm/dd/yy.");
    properties.put("gfdf-invalidformat-i", "$S1 is incorrectly formatted.  The correct format is mm/dd/yy.");
    properties.put("gfdf-invalidformat-l", "$S1 field's date is incorrectly formatted.  The correct format is mm/dd/yy.");
  
    // FormEmailAddressField (feaf)
    
    properties.put("gfeaf-empty-e", "$S1 is empty.");
    properties.put("gfeaf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfeaf-empty-l", "Please provide input in this field.");
    properties.put("gfeaf-long-e", "$S1 is too long.");
    properties.put("gfeaf-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gfeaf-long-l", "Please keep the input in this field to $S2 characters or less.");
    properties.put("gfeaf-invalidformat-e", "$S1 is incorrectly formatted.  Format: username@domain.com");
    properties.put("gfeaf-invalidformat-i", "$S1 is incorrectly formatted.  The correct format is username@domain.com");
    properties.put("gfeaf-invalidformat-l", "$S1 field's email address is incorrectly formatted.  The correct format is username@domain.com");
    
    // FormPhoneNumberField (fpnf)
    
    properties.put("gfpnf-empty-e", "$S1 is empty.");
    properties.put("gfpnf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfpnf-empty-l", "Please provide input in this field.");
    properties.put("gfpnf-long-e", "$S1 is too long.");
    properties.put("gfpnf-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gfpnf-long-l", "Please keep the input in this field to $S2 characters or less.");
    properties.put("gfpnf-invalidformat-e", "$S1 is incorrectly formatted.  Format: (123) 456-7890");
    properties.put("gfpnf-invalidformat-i", "$S1 is incorrectly formatted.  The correct format is (123) 456-7890");
    properties.put("gfpnf-invalidformat-l", "$S1 field's phone number is incorrectly formatted.  The correct format is (123) 456-7890");
    
    // FormCheckBoxGroup (fcbg)

    properties.put("gfcbg-lowsingular-e", "$S1 does not have enough options selected.");
    properties.put("gfcbg-lowsingular-i", "Please select at least 1 option from the check boxes named $S1.");
    properties.put("gfcbg-lowsingular-l", "These check boxes require 1 selection.");
    properties.put("gfcbg-lowplural-e", "$S1 does not have enough options selected.");
    properties.put("gfcbg-lowplural-i", "Please select at least $S2 options from the check boxes named $S1.");
    properties.put("gfcbg-lowplural-l", "These check boxes require $S2 selections.");
    properties.put("gfcbg-highsingular-e", "$S1 is too many selections.");
    properties.put("gfcbg-highsingular-i", "Please limit yourself to 1 selection from the check boxes named $S1.");
    properties.put("gfcbg-highsingular-l", "These check boxes are limited to 1 selection.");
    properties.put("gfcbg-highplural-e", "$S1 is too many selections.");
    properties.put("gfcbg-highplural-i", "Please limit yourself to $S3 selections from the check boxes named $S1.");
    properties.put("gfcbg-highplural-l", "These check boxes are limited to $S3 selections.");
    
    // FormPasswordField (fpf)
    
    properties.put("gfpf-empty-e", "$S1 is empty.");
    properties.put("gfpf-empty-i", "Please provide input in the field named $S1.");
    properties.put("gfpf-empty-l", "Please provide input in this field.");
    properties.put("gfpf-short-e", "$S1 is too short.");
    properties.put("gfpf-short-i", "Please specify at least $S2 characters in the field named $S1.");
    properties.put("gfpf-short-l", "Please specify at least $S2 characters.");
    properties.put("gfpf-long-e", "$S1 is too long.");
    properties.put("gfpf-long-i", "Please keep the input in field $S1 to $S2 characters or less.");
    properties.put("gfpf-long-l", "Please keep the input in this field to $S2 characters or less.");
    properties.put("gfpf-mismatch-e", "$S1 doesn't match.");
    properties.put("gfpf-mismatch-i", "The passwords you have entered do not match.");
    properties.put("gfpf-mismatch-l", "The passwords you have entered do not match.");
    properties.put("gfpf-mismatchCase-e", "$S1 doesn't match.");
    properties.put("gfpf-mismatchCase-i", "The passwords you have entered do not match. Please pay attention to capitalization.");
    properties.put("gfpf-mismatchCase-l", "The passwords you have entered do not match. Please pay attention to capitalization.");
    properties.put("gfpf-badchars-e", "$S1 contains non-alphanumeric characters.");
    properties.put("gfpf-badchars-i", "$S1 can only be made up of letters and numbers.");
    properties.put("gfpf-badchars-l", "This field can only be made up of letters and numbers.");
    
    properties.put("gfmppnf-missing-e", "$S1 is incomplete.");
    properties.put("gfmppnf-missing-i", "Please provide the entire phone number including area code for the field named $S1.");
    properties.put("gfmppnf-missing-l", "Please provide the entire phone number including area code for this field.");
    
    properties.put("gfssn-missing-e", "$S1 is incomplete.");
    properties.put("gfssn-missing-i", "Please completely fill out the field named $S1.");
    properties.put("gfssn-missing-l", "Please completely fill out this field.");
    
    properties.put("gfmppnf-extension-missing-e", "$S1 is empty.");
    properties.put("gfmppnf-extension-missing-i", "Please provide input for the field $S1.");
    properties.put("gfmppnf-extension-missing-l", "Please provide input for this field.");

    // FormNonce (fnonce)
    
    properties.put("gfnonce-e", "Form ticket is missing.");
    properties.put("gfnonce-i", "Please submit input using the form provided.");
    properties.put("gfnonce-l", "Please submit input using the form provided.");
    
    // FormURLField

    properties.put("gfurlf-malformed-e", "$S1 is not a valid URL.");
    properties.put("gfurlf-malformed-i", "Please enter a valid URL for $S1. URLs should be of the form: http://www.yourwebsite.com");
    properties.put("gfurlf-malformed-l", "Please enter a valid URL. URLs should be of the form: http://www.yourwebsite.com");

    // PasswordAuthenticationValidator
    properties.put("gfpav-badpassword-e", "$S1 does not contain the correct password.");
    properties.put("gfpav-badpassword-i", "Please provide your current password in $S1.");
    properties.put("gfpav-badpassword-l", "Please provide your current password.");
   
  } // end constructFormElementResources
  
  /**
   * Adds a country to two StringLists (one for codes, one for names)
   */
  protected void addCountryToLists(Country country, StringList codes, StringList names)
  {
    codes.add(country.code);
    names.add(country.name);
  }
  
  /**
   * Contains a country's full name and its two-letter ISO code.
   */
  private static class Country
  {
    private String code;
    private String name;
    
    public Country(String code, String name)
    {
      this.code = code;
      this.name = name;
    }
  }
  
  /**
   * Standard toString.
   */
  @Override
  public String toString()
  {
    return "DefaultGeminiResources [locale: US English]";
  }

}   // End DefaultGeminiResources.
