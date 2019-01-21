from setuptools import setup

dependencies = [
    "adapt-mock-icd",
    "adaptmb"
]

setup(name='adapt-mock-product-ingestor',
      version='0.1',
      description='DRAWS Mock Product Ingestor',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['adapt', 'adapt/mock', 'adapt/mock/product-ingestor'],
      install_requires=dependencies,
      zip_safe=False)
