import { NavItem } from './nav-item/nav-item';

export const navItems: NavItem[] = [
  {
    navCap: 'Home',
  },
  {
    displayName: 'Dashboard',
    iconName: 'solar:widget-add-line-duotone',
    route: '/dashboard',
  }, 
   {
    displayName: 'Metadata upload',
    iconName: 'solar:widget-add-line-duotone',
    route: '/dashboard/upload',
  }, 
  {
    navCap: 'Analysis Data',
    divider: true
  },
  
  {
    displayName: 'Analyses Dashboard',
    iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/analysis',
  },  
  {
    displayName: 'Alignment',
    iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/alignment',
  },
  {
    displayName: 'Post-processing',
    iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/postProcessing',
  },
  {
    navCap: 'FHIR Data',
    divider: true
  },
  {
    displayName: 'Sample',
    iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/sample',
  },{
    displayName: 'Patients',
    iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/patients',
  },
  {
    displayName: 'FHIR Tree',
     iconName: 'solar:widget-add-line-duotone',
    route: '/fhirData/fhirTree',

  },
  {
    navCap: 'Contact',
    divider: true
  },
  {
    displayName: 'About Us',
    iconName: 'solar:widget-add-line-duotone',
    route: '/dashboard/aboutUs',
  },
  {
    isLogo: true,  
    logoPath: 'assets/newLogo/logo.jpg'  
  }

];
