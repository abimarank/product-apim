/*
 *   Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *   WSO2 Inc. licenses this file to you under the Apache License,
 *   Version 2.0 (the "License"); you may not use this file except
 *   in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.wso2.carbon.apimgt.migration.client._110Specific;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Comment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.wso2.carbon.apimgt.impl.APIConstants;
import org.wso2.carbon.apimgt.migration.APIMigrationException;
import org.wso2.carbon.apimgt.migration.util.Constants;
import org.wso2.carbon.apimgt.migration.util.ResourceUtil;

import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.charset.Charset;

public class ResourceModifier {
    public static String modifyWorkFlowExtensions(String xmlContent) throws APIMigrationException {
        Writer stringWriter = new StringWriter();

        Document doc = ResourceUtil.buildDocument(xmlContent, APIConstants.WORKFLOW_EXECUTOR_LOCATION);

        if (doc != null) {
            Element rootElement = doc.getDocumentElement();

            Element subscriptionElement = doc.createElement(Constants.WF_SUBSCRIPTION_DELETION_TAG);
            subscriptionElement.setAttribute(Constants.WF_EXECUTOR_ATTRIBUTE, Constants.WF_SUBSCRIPTION_DELETION_CLASS);
            Comment subscriptionTagComment = doc.createComment(Constants.WF_SUBSCRIPTION_DELETION_TAG_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_SUBSCRIPTION_SERVICE_ENDPOINT_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_USERNAME_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_PASSWORD_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_CALLBACK_URL_COMMENT +
                    System.lineSeparator() + Constants.WF_SUBSCRIPTION_DELETION_CLOSING_TAG_COMMENT);


            rootElement.appendChild(subscriptionElement);
            rootElement.appendChild(subscriptionTagComment);

            Element applicationElement = doc.createElement(Constants.WF_APPLICATION_DELETION_TAG);
            applicationElement.setAttribute(Constants.WF_EXECUTOR_ATTRIBUTE, Constants.WF_APPLICATION_DELETION_CLASS);
            Comment applicationTagComment = doc.createComment(Constants.WF_APPLICATION_DELETION_TAG_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_APPLICATION_SERVICE_ENDPOINT_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_USERNAME_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_PASSWORD_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_CALLBACK_URL_COMMENT +
                    System.lineSeparator() + Constants.WF_COMMENT_INDENT + Constants.WF_APPLICATION_DELETION_CLOSING_TAG_COMMENT);

            rootElement.appendChild(applicationElement);
            rootElement.appendChild(applicationTagComment);
            try {
                doc.getDocumentElement().normalize();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, Charset.defaultCharset().toString());
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
            } catch (TransformerException e) {
                ResourceUtil.handleException("Error occurred while saving modified " + APIConstants.WORKFLOW_EXECUTOR_LOCATION, e);
            }
        }

        return stringWriter.toString();
    }

    public static String modifyTiers(String xmlContent, String fileName) throws APIMigrationException {
        Writer stringWriter = new StringWriter();

        Document doc = ResourceUtil.buildDocument(xmlContent, fileName);

        if (doc != null) {
            Element rootElement = doc.getDocumentElement();

            Element throttleAssertion = (Element) rootElement.getElementsByTagNameNS(Constants.TIER_THROTTLE_XMLNS,
                                                                Constants.TIER_MEDIATOR_THROTTLE_ASSERTION_TAG).item(0);

            NodeList tierNodes = throttleAssertion.getChildNodes();

            for (int i = 0; i < tierNodes.getLength(); ++i) {
                Node tierNode = tierNodes.item(i);

                if (tierNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element tierTag = (Element) tierNode;

                    Element controlTag = (Element) tierTag.getElementsByTagNameNS(Constants.TIER_THROTTLE_XMLNS,
                                                                        Constants.TIER_CONTROL_TAG).item(0);

                    Element controlPolicyTag = (Element) controlTag.getElementsByTagNameNS(Constants.TIER_WSP_XMLNS,
                                                                        Constants.TIER_POLICY_TAG).item(0);

                    NodeList controlPolicyPolicyTags = controlPolicyTag.getElementsByTagNameNS(Constants.TIER_WSP_XMLNS,
                                                                                Constants.TIER_POLICY_TAG);

                    for (int j = 0; j < controlPolicyPolicyTags.getLength(); ++j) {
                        if (controlPolicyPolicyTags.item(j).getNodeType() == Node.ELEMENT_NODE) {
                            NodeList attributeTags = ((Element) controlPolicyPolicyTags.item(j)).
                                    getElementsByTagNameNS(Constants.TIER_THROTTLE_XMLNS, Constants.TIER_ATTRIBUTES_TAG);

                            if (attributeTags.getLength() > 0) {
                                updateTierAttributes(doc, attributeTags.item(0));
                            }
                            break;
                        }
                    }

                    if (controlPolicyPolicyTags.getLength() == 0) {
                        addTierAttributes(doc, controlPolicyTag);
                    }
                }
            }

            try {
                doc.getDocumentElement().normalize();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, Charset.defaultCharset().toString());
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
            } catch (TransformerException e) {
                ResourceUtil.handleException("Error occurred while saving modified " + APIConstants.WORKFLOW_EXECUTOR_LOCATION, e);
            }
        }

        return stringWriter.toString();
    }


    public static String removeExecutorsFromAPILifeCycle(String apiLifeCycle) throws APIMigrationException {
        Writer stringWriter = new StringWriter();

        Document doc = ResourceUtil.buildDocument(apiLifeCycle, "APILifeCycle");

        if (doc != null) {
            Element rootElement = doc.getDocumentElement();

            NodeList stateTags = rootElement.getElementsByTagName(Constants.API_LIFE_CYCLE_STATE_TAG);

            for (int i = 0; i < stateTags.getLength(); ++i) {
                Element stateTag = (Element) stateTags.item(i);

                NodeList dataModelTags = stateTag.getElementsByTagName(Constants.API_LIFE_CYCLE_DATA_MODEL_TAG);

                if (dataModelTags.getLength() > 0) {
                    Element dataModelTag = (Element) dataModelTags.item(0);
                    dataModelTag.getParentNode().removeChild(dataModelTag);
                }
            }

            try {
                doc.getDocumentElement().normalize();
                Transformer transformer = TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(OutputKeys.ENCODING, Charset.defaultCharset().toString());
                transformer.setOutputProperty(OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
                transformer.transform(new DOMSource(doc), new StreamResult(stringWriter));
            }
            catch (TransformerException e) {
                ResourceUtil.handleException("Error occurred while transforming modified " + "APILifeCycle", e);
            }
        }

        return stringWriter.toString();
    }


    private static void addTierAttributes(Document doc, Element controlPolicyTag) {
        Element policyTag = doc.createElementNS(Constants.TIER_WSP_XMLNS, Constants.TIER_POLICY_TAG);
        policyTag.setPrefix(Constants.TIER_WSP_XMLNS_VAR);

        Element attributesTag = doc.createElementNS(Constants.TIER_THROTTLE_XMLNS, Constants.TIER_ATTRIBUTES_TAG);
        attributesTag.setPrefix(Constants.TIER_THROTTLE_XMLNS_VAR);

        Element billingPlanTag = doc.createElementNS(Constants.TIER_THROTTLE_XMLNS,
                                                        APIConstants.THROTTLE_TIER_PLAN_ATTRIBUTE);
        billingPlanTag.setPrefix(Constants.TIER_THROTTLE_XMLNS_VAR);
        billingPlanTag.setTextContent(Constants.TIER_BILLING_PLAN_FREE);

        attributesTag.appendChild(billingPlanTag);

        Element stopOnQuotaTag = doc.createElementNS(Constants.TIER_THROTTLE_XMLNS,
                                                    APIConstants.THROTTLE_TIER_QUOTA_ACTION_ATTRIBUTE);
        stopOnQuotaTag.setPrefix(Constants.TIER_THROTTLE_XMLNS_VAR);
        stopOnQuotaTag.setTextContent(Constants.TIER_STOP_ON_QUOTA_TRUE);

        attributesTag.appendChild(stopOnQuotaTag);

        policyTag.appendChild(attributesTag);
        controlPolicyTag.appendChild(policyTag);
    }

    private static void updateTierAttributes(Document doc, Node attributesTag) {
        if (!isTagExists(attributesTag, Constants.TIER_THROTTLE_XMLNS,
                APIConstants.THROTTLE_TIER_PLAN_ATTRIBUTE)) {
            Element billingPlanTag = doc.createElementNS(Constants.TIER_THROTTLE_XMLNS,
                    APIConstants.THROTTLE_TIER_PLAN_ATTRIBUTE);
            billingPlanTag.setPrefix(Constants.TIER_THROTTLE_XMLNS_VAR);
            billingPlanTag.setTextContent(Constants.TIER_BILLING_PLAN_FREE);
            attributesTag.appendChild(billingPlanTag);
        }

        if (!isTagExists(attributesTag, Constants.TIER_THROTTLE_XMLNS,
                APIConstants.THROTTLE_TIER_QUOTA_ACTION_ATTRIBUTE)) {
            Element stopOnQuotaTag = doc.createElementNS(Constants.TIER_THROTTLE_XMLNS,
                    APIConstants.THROTTLE_TIER_QUOTA_ACTION_ATTRIBUTE);
            stopOnQuotaTag.setPrefix(Constants.TIER_THROTTLE_XMLNS_VAR);
            stopOnQuotaTag.setTextContent(Constants.TIER_STOP_ON_QUOTA_TRUE);
            attributesTag.appendChild(stopOnQuotaTag);
        }
    }


    private static boolean isTagExists(Node parentTag, String namespaceURI, String childTagName) {
        NodeList nodes = ((Element) parentTag).getElementsByTagNameNS(namespaceURI, childTagName);

        return nodes.getLength() > 0;

    }
}
